# ============================================================================
# ECS FARGATE + ALB — the eight services as serverless containers.
# ============================================================================
# compose -> AWS translation:
#   docker compose up      -> ECS Service (desired_count, rolling deploys)
#   image build            -> ECR repository + CI push (not modeled here)
#   ports/healthcheck      -> target group health checks
#   depends_on             -> service ordering + target-group registration
#   nginx front door       -> ALB listener rules (path-based, like the gateway)
#
# ONE task definition per service would be the full expansion; to keep this
# file teachable we define the pattern ONCE (api-gateway, fully annotated)
# and generate the rest with for_each — the same DRY instinct Terraform has
# over hand-written CloudFormation.
# ============================================================================

resource "aws_ecs_cluster" "securebank" {
  name = "${var.environment}-securebank"

  setting {
    name  = "containerInsights"
    value = "enabled"   # per-task metrics in CloudWatch — observability from day 1
  }
}

# --- ALB: the sanctioned public door ------------------------------------------
resource "aws_lb" "edge" {
  name               = "${var.environment}-securebank"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id   # ALB in PUBLIC subnets; tasks stay private
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.edge.arn
  port              = 443
  protocol          = "HTTPS"
  # certificate_arn = aws_acm_certificate.securebank.arn  (cert mgmt omitted)
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.gateway.arn
  }
}

# --- target groups: one per service the ALB can reach --------------------------
resource "aws_lb_target_group" "gateway" {
  name        = "${var.environment}-gateway"
  port        = 8080
  protocol    = "HTTP"
  vpc_id      = aws_vpc.securebank.id
  target_type = "ip"          # Fargate tasks register by ENI IP, not instance

  health_check {
    path                = "/actuator/health/readiness"   # the SAME probe compose/k8s use
    matcher             = "200"
    interval            = 15
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  # Deregistration delay: in-flight requests finish before a task dies
  # (graceful shutdown, the cloud version of Spring's graceful-stop).
  deregistration_delay = 30
}

# --- IAM: the task role is the service's IDENTITY in AWS ------------------------
resource "aws_iam_role" "task_execution" {
  name = "${var.environment}-securebank-task-execution"
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = { Service = "ecs-tasks.amazonaws.com" }
      Action = "sts:AssumeRole"
    }]
  })
}

resource "aws_iam_role_policy_attachment" "task_execution" {
  role       = aws_iam_role.task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
  # Pulls images + writes logs. Anything MORE (reading secrets, publishing to
  # SNS/SQS) gets its OWN least-privilege policy — the task ROLE (identity of
  # the app) vs EXECUTION role (identity of the platform) distinction.
}

# --- the pattern, instantiated for all eight services ---------------------------
locals {
  services = {
    "auth-service"         = 8081
    "account-service"      = 8082
    "payment-service"      = 8083
    "loan-service"         = 8084
    "notification-service" = 8085
    "api-gateway"          = 8080
    "discovery-service"    = 8761
    "config-server"        = 8888
  }
}

resource "aws_ecs_task_definition" "service" {
  for_each = local.services

  family                   = "${var.environment}-${each.key}"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"          # every task gets its own ENI => SGs apply per task
  cpu                      = var.ecs_cpu
  memory                   = var.ecs_memory
  execution_role_arn       = aws_iam_role.task_execution.arn

  container_definitions = jsonencode([{
    name      = each.key
    image     = "123456789012.dkr.ecr.${var.aws_region}.amazonaws.com/securebank/${each.key}:${var.container_image_tag}"
    essential = true

    portMappings = [{ containerPort = each.value, protocol = "tcp" }]

    environment = [
      { name = "CONFIG_SERVER_URL", value = "http://config-server:8888" },
      { name = "SPRING_PROFILES_ACTIVE", value = "cloud" },
    ]
    # Secrets arrive from Secrets Manager at task start — values never in the
    # task definition (which IS visible in the console/API/logs).
    secrets = [
      { name = "JWT_SECRET", valueFrom = "arn:aws:secretsmanager:${var.aws_region}:123456789012:secret:securebank/jwt" },
    ]

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        "awslogs-group"         = "/ecs/${var.environment}/${each.key}"
        "awslogs-region"        = var.aws_region
        "awslogs-stream-prefix" = "app"
      }
    }
  }])
}

resource "aws_ecs_service" "service" {
  for_each = local.services

  name            = each.key
  cluster         = aws_ecs_cluster.securebank.id
  task_definition = aws_ecs_task_definition.service[each.key].arn
  desired_count   = var.desired_task_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id        # PRIVATE: no inbound internet path
    security_groups  = [aws_security_group.tasks.id]
    assign_public_ip = false                          # the flag that must never flip
  }

  # Only the gateway is wired to the ALB here; internal services stay
  # reachable solely via VPC DNS / Cloud Map (service discovery), mirroring
  # the "no route for /internal" perimeter from stage 5.
  dynamic "load_balancer" {
    for_each = each.key == "api-gateway" ? [1] : []
    content {
      target_group_arn = aws_lb_target_group.gateway.arn
      container_name   = "api-gateway"
      container_port   = 8080
    }
  }

  deployment_minimum_healthy_percent = 100   # never dip below desired during rollout
  deployment_maximum_percent         = 200   # blue/green-ish surge with double capacity
}
