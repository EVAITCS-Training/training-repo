# ============================================================================
# RDS POSTGRES — managed database in PRIVATE subnets (the production answer
# to infra/k8s/10-postgres.yaml's "don't run your own database" lecture).
# ============================================================================
# What AWS takes off your plate here: backups + PITR, minor-version patching,
# storage growth, failover (multi_az), encryption at rest. What stays yours:
# schema migrations (Flyway per service!), credential rotation, query tuning.
# ============================================================================

resource "aws_db_subnet_group" "private" {
  name       = "${var.environment}-securebank-db"
  subnet_ids = aws_subnet.private[*].id   # database reachable ONLY from private subnets
}

resource "aws_db_instance" "postgres" {
  identifier = "${var.environment}-securebank"

  engine         = "postgres"
  engine_version = "17"
  instance_class = var.db_instance_class

  allocated_storage     = 20
  max_allocated_storage = 100            # storage autoscaling — one less 2am page
  storage_encrypted     = true           # encryption at rest, non-negotiable (Week 9)

  db_name  = "postgres"                  # per-service DATABASES are created by
  username = "securebank"                # migrations/provisioning, not by Terraform
  password = "CHANGE-ME-VIA-SECRETS-MANAGER"
  # ^ Lecture note: a literal password in tf is a teaching scar. Real setups
  #   use manage_master_user_password = true (Secrets Manager integration) so
  #   the secret never touches state or git.

  db_subnet_group_name   = aws_db_subnet_group.private.name
  vpc_security_group_ids = [aws_security_group.database.id]
  publicly_accessible    = false         # the line that must never flip

  multi_az               = var.environment == "prod" ? true : false
  backup_retention_period = 7
  skip_final_snapshot    = var.environment != "prod"
  final_snapshot_identifier = var.environment == "prod" ? "${var.environment}-securebank-final" : null

  # Schema ownership belongs to the SERVICES (database-per-service): each
  # service's Flyway migrations create its own database/schema at deploy time.
  # Terraform provisions the INSTANCE; the app provisions its STRUCTURE.
}
