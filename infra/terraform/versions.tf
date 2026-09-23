# ============================================================================
# PROVIDER PINS — reproducibility is the whole point of IaC (Week 8).
# ============================================================================
# Without these blocks, `terraform init` picks whatever provider version is
# newest that day: plans differ between laptops, and one minor upgrade can
# change resource behavior. Pin majors, allow patches.
terraform {
  required_version = ">= 1.7"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  # STATE: the file that remembers what Terraform built. Local state is fine
  # for learning; a TEAM needs remote state (S3 + DynamoDB lock, or Terraform
  # Cloud) or two people will race each other into duplicate VPCs.
  # backend "s3" { bucket = "..."; key = "securebank/terraform.tfstate"; region = "..."; dynamodb_table = "..." }
}

provider "aws" {
  region = var.aws_region
  # Credentials come from the environment / shared config / IAM role —
  # NEVER from variables or files in the repo (Week 1 secrets rule, again).
  default_tags {
    tags = {
      Project   = "securebank"
      Course    = "evaitcs-9week"
      ManagedBy = "terraform"   # humans learn to keep their hands off
    }
  }
}
