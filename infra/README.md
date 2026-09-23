# infra/ — Deployment artifacts (Weeks 8-9: AWS, Kubernetes, IaC, Hardening)

**TEACHING SAMPLES — nothing here is applied by any stage's compose stack.**
These files are the "where does this run in production?" answer to every
"but in real life...?" question raised in stages 1-5. Read them as annotated
design documents; `terraform validate` and `kubectl apply --dry-run=client`
are the only safe verbs.

## Layout

```
infra/
├── k8s/                     # Kubernetes manifests (the compose stack, translated)
│   ├── 00-namespace.yaml    # isolation boundary for the whole app
│   ├── 10-postgres.yaml     # StatefulSet + PVC + Secret (with the "use RDS" lecture)
│   ├── 20-microservices.yaml# Deployments + Services for all 8 services
│   ├── 30-ingress.yaml      # the cluster's front door -> api-gateway
│   ├── 40-networkpolicy.yaml# /internal/** protection done PROPERLY (stage-5 gap closed)
│   └── 50-hpa.yaml          # autoscaling account-service on CPU
└── terraform/               # AWS as code (Week 8: IaC, AWS Deployment)
    ├── versions.tf          # provider pins
    ├── variables.tf         # every knob, typed + documented
    ├── vpc.tf               # public/private subnets, NAT — the guide's VPC diagram
    ├── database.tf          # RDS Postgres in private subnets
    ├── ecs.tf               # Fargate cluster + services + ALB
    └── outputs.tf           # what a human needs after `terraform apply`
```

## How the stages map onto this folder

| Stage concept | Compose equivalent | K8s equivalent | AWS equivalent |
|---|---|---|---|
| service | `services:` entry | Deployment + Service | ECS task + service |
| network alias `backend` | compose networks | Service DNS name | ALB target group |
| healthcheck | compose healthcheck | liveness/readiness probes | target group health check |
| volumes | named volumes | PVC / StatefulSet | EBS / RDS storage |
| env injection | `environment:` | ConfigMap/Secret env | SSM/Secrets Manager + task env |
| `/internal` perimeter | absent gateway route | NetworkPolicy | security groups |
| scale up | `--scale` (breaks ports!) | replicas + HPA | ECS desired count + target tracking |

## Exercises

1. `kubectl apply --dry-run=client -f k8s/` against a local cluster
   (`kind` or Docker Desktop's K8s) — then actually deploy ONE service and
   watch DNS + probes replace compose's healthchecks.
2. Port the notification-service Deployment to use a Kafka Secret instead of
   a plaintext env value; explain why ConfigMap ≠ Secret.
3. In terraform/, add a second availability zone and watch which resources
   the plan changes (subnets, NAT, RDS multi-AZ).
4. Write the NetworkPolicy that lets ONLY payment-service pods reach
   account-service:8082, then break it on purpose and observe the timeout.
