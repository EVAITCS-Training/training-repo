# ============================================================================
# OUTPUTS — what a human needs AFTER apply (and what CI pipes into deploys).
# ============================================================================
# Rule of thumb: output CONNECTION information freely; never output secrets
# (a sensitive value printed by `terraform output` still lands in CI logs).
# ============================================================================

output "vpc_id" {
  description = "VPC for cross-stack references (e.g. a bastion or VPN stack)."
  value       = aws_vpc.securebank.id
}

output "alb_dns_name" {
  description = "Point your DNS CNAME here; this is the public entry point."
  value       = aws_lb.edge.dns_name
}

output "rds_endpoint" {
  description = "Private endpoint; reachable only from inside the VPC (psql via bastion/VPN)."
  value       = aws_db_instance.postgres.endpoint
}

output "ecs_cluster_name" {
  description = "For aws ecs describe-services / deploy scripts."
  value       = aws_ecs_cluster.securebank.name
}

output "service_names" {
  description = "The eight ECS services, for rollout orchestration."
  value       = [for s in aws_ecs_service.service : s.name]
}
