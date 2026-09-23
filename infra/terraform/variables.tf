# ============================================================================
# VARIABLES — every knob, typed, documented, defaulted.
# ============================================================================
# The discipline: anything environment-specific (region, sizes, counts) is a
# variable; anything architectural (topology) is code. Values per environment
# live in *.tfvars files (dev.tfvars, prod.tfvars) that are NOT secrets.
# ============================================================================

variable "aws_region" {
  description = "Region for all resources. Choose once; migrating regions later is a project of its own."
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name; prefixes resource names so dev and prod can coexist."
  type        = string
  default     = "dev"
}

variable "vpc_cidr" {
  description = "Address space for the VPC. /16 = 65k IPs; plan subnet math BEFORE picking (Week 9)."
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "AZs for subnets. Two is the minimum for anything called 'production'."
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "db_instance_class" {
  description = "RDS instance size. dev: burstable; prod: memory-optimized for connection-heavy workloads."
  type        = string
  default     = "db.t4g.micro"
}

variable "ecs_cpu" {
  description = "Fargate task CPU units (256 = 0.25 vCPU)."
  type        = number
  default     = 512
}

variable "ecs_memory" {
  description = "Fargate task memory in MiB. JVM services want >= 1024 (heap + metaspace + threads)."
  type        = number
  default     = 1024
}

variable "desired_task_count" {
  description = "Running tasks per service. ECS target-tracking can adjust this later."
  type        = number
  default     = 1
}

variable "container_image_tag" {
  description = "Image tag deployed by ECS tasks. CI writes this (via workspace/vars), humans read it."
  type        = string
  default     = "latest" # pin to a digest in anything real; 'latest' is a lecture shortcut
}
