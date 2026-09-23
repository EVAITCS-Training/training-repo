# ============================================================================
# VPC — the network blueprint from the Week 8 "Typical SecureBank VPC layout".
# ============================================================================
#   Internet Gateway
#        │
#   PUBLIC subnets  (ALB only — nothing else faces the internet)
#        │
#   Application Load Balancer
#        │
#   PRIVATE subnets (ECS tasks, RDS) — outbound via NAT Gateway
#
# The rule that matters: compute and data live in PRIVATE subnets with no
# route in from the internet. The ALB is the single sanctioned door. Every
# "how did that get exposed?" post-mortem starts with someone putting an app
# in a public subnet "temporarily".
# ============================================================================

resource "aws_vpc" "securebank" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true   # required for RDS/ECS service discovery names
}

# --- public subnets: ALB lives here ------------------------------------------
resource "aws_subnet" "public" {
  count                   = length(var.availability_zones)
  vpc_id                  = aws_vpc.securebank.id
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)          # 10.0.0.0/24, 10.0.1.0/24
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = false   # even public-subnet resources get IPs via the ALB/NAT, not per-ENI
}

# --- private subnets: tasks + database ---------------------------------------
resource "aws_subnet" "private" {
  count             = length(var.availability_zones)
  vpc_id            = aws_vpc.securebank.id
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 100 + count.index)          # 10.0.100.0/24, ...
  availability_zone = var.availability_zones[count.index]
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.securebank.id
}

# NAT Gateway: private subnets need OUTBOUND (pull images, call AWS APIs) but
# no inbound path. One NAT per AZ is the production pattern (cost!); one
# shared is the dev compromise — the trade-off is an availability boundary.
resource "aws_eip" "nat" {
  domain = "vpc"
}

resource "aws_nat_gateway" "nat" {
  allocation_id = aws_eip.nat.id
  subnet_id     = aws_subnet.public[0].id
}

# --- route tables: the actual "who can reach whom" ----------------------------
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.securebank.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.securebank.id
  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.nat.id   # outbound only; no IGW route exists here
  }
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table_association" "private" {
  count          = length(aws_subnet.private)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

# --- security groups: instance-level firewalls (the AWS twin of NetworkPolicy)
resource "aws_security_group" "alb" {
  name_prefix = "${var.environment}-alb-"
  vpc_id      = aws_vpc.securebank.id

  ingress {
    description = "HTTPS from anywhere (TLS terminates at the ALB)"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "tasks" {
  name_prefix = "${var.environment}-tasks-"
  vpc_id      = aws_vpc.securebank.id

  ingress {
    description     = "Only the ALB may reach task ports"
    from_port       = 8080
    to_port         = 8085
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]   # SG-to-SG reference, not CIDR: moves with the ALB
  }
  ingress {
    description = "Service-to-service (saga internal calls) stays inside the SG"
    from_port   = 8080
    to_port     = 8085
    protocol    = "tcp"
    self        = true
  }
  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "database" {
  name_prefix = "${var.environment}-db-"
  vpc_id      = aws_vpc.securebank.id

  ingress {
    description     = "Postgres from tasks ONLY"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.tasks.id]
  }
  # No egress rule needed (responses are stateful); no 0.0.0.0/0 anywhere near 5432.
}
