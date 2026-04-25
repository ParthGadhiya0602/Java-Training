---
title: "Module 58 — Cloud Deployment"
nav_order: 58
render_with_liquid: false
---

[View source on GitHub](https://github.com/ParthGadhiya0602/Java-Training/tree/main/module-58-cloud-deployment/src){: .btn .btn-outline }

# Module 58 — Cloud Deployment

## What this module covers

Deploying Spring Boot to AWS (ECS Fargate) and GCP (App Engine / GKE), the Heroku
12-factor app methodology, Spring Boot profile-based configuration, Kubernetes liveness
and readiness probes, and graceful shutdown. Tests verify config loading, external
overrides, health indicators, and shutdown mode.

---

## Project structure

```
src/main/java/com/javatraining/cloud/
├── CloudDeploymentApplication.java
├── config/
│   └── AppProperties.java          # @ConfigurationProperties — 12-factor Factor III
├── health/
│   └── AppHealthIndicator.java     # custom HealthIndicator for /actuator/health
└── api/
    └── DeploymentController.java   # GET /api/deployment/info

src/main/resources/
├── application.properties          # base config: graceful shutdown, actuator, app.*
├── application-staging.properties  # staging overrides (APP_ENVIRONMENT=staging)
└── application-prod.properties     # prod overrides (all secrets via env vars)

deployment/
├── aws/
│   ├── ecs-task-definition.json    # Fargate task: env vars, secrets, health check, logging
│   └── buildspec.yml               # CodeBuild: test → build image → push to ECR
├── gcp/
│   ├── app.yaml                    # App Engine Flexible: scaling, probes
│   └── cloudbuild.yaml             # Cloud Build: test → build → push to GCR → deploy to GKE
└── k8s/
    ├── deployment.yaml             # Deployment: probes, preStop hook, resource limits
    └── service.yaml                # LoadBalancer Service

src/test/java/com/javatraining/cloud/
├── DeploymentInfoTest.java         # endpoint + actuator/info (2 tests)
├── EnvironmentOverrideTest.java    # 12-factor config override (1 test)
├── HealthIndicatorTest.java        # custom indicator + actuator/health (2 tests)
└── GracefulShutdownTest.java       # server.shutdown=graceful (1 test)
```

---

## 12-factor app — all 12 factors

| # | Factor | How this module applies it |
|---|--------|---------------------------|
| I | Codebase | One repo, one artifact; environment is a variable, not a branch |
| II | Dependencies | All deps explicit in `pom.xml`; no system-wide installs |
| III | Config | `AppProperties` loads from `app.*`; any property overridable via `APP_*` env var |
| IV | Backing services | DB URL, passwords come from env vars / secrets manager — swappable without code change |
| V | Build, release, run | `mvn package` (build) → image tag (release) → container start (run); strictly separate |
| VI | Processes | Stateless: no session data in memory; any pod can handle any request |
| VII | Port binding | `server.port=8080`; the app IS the server — no external web server needed |
| VIII | Concurrency | Scale out by adding pods; Java 21 virtual threads for within-process concurrency |
| IX | Disposability | `server.shutdown=graceful` + 30s drain timeout + Kubernetes `preStop` sleep |
| X | Dev/prod parity | Same Docker image in every environment; only env vars change |
| XI | Logs | Spring Boot logs to stdout by default; CloudWatch / Cloud Logging aggregates |
| XII | Admin processes | `mvn flyway:migrate`, `mvn versions:set` run as one-off jobs, not app startup |

---

## Factor III — Config in detail

### @ConfigurationProperties record

```java
@ConfigurationProperties(prefix = "app")
@Validated
public record AppProperties(
        @NotBlank String name,
        @NotBlank String version,
        @NotBlank String environment,
        String region
) {}
```

Spring Boot's relaxed binding maps environment variables to properties automatically:

| Environment variable | Property |
|---|---|
| `APP_ENVIRONMENT` | `app.environment` |
| `APP_REGION` | `app.region` |
| `APP_VERSION` | `app.version` |

Priority (highest wins): env vars > system properties > `@TestPropertySource` > profile properties > `application.properties`.

### Profile-based config

```
application.properties          ← always loaded
application-staging.properties  ← loaded when SPRING_PROFILES_ACTIVE=staging
application-prod.properties     ← loaded when SPRING_PROFILES_ACTIVE=prod
```

Activate at startup:
```bash
# Docker / Kubernetes
APP_ENVIRONMENT=prod SPRING_PROFILES_ACTIVE=prod java -jar app.jar

# Local staging test
SPRING_PROFILES_ACTIVE=staging ./mvnw spring-boot:run
```

### Config in ECS task definition

```json
"environment": [
  { "name": "APP_ENVIRONMENT", "value": "prod" },
  { "name": "APP_REGION",      "value": "us-east-1" }
],
"secrets": [
  {
    "name": "APP_DB_PASSWORD",
    "valueFrom": "arn:aws:secretsmanager:us-east-1:ACCOUNT_ID:secret:app/db-password"
  }
]
```

`environment` for non-sensitive values, `secrets` for passwords/keys — ECS fetches
secrets from Secrets Manager at task startup; the app sees them as plain env vars.

---

## Factor IX — Disposability: graceful shutdown

```properties
server.shutdown=graceful
spring.lifecycle.timeout-per-shutdown-phase=30s
```

On SIGTERM, Spring Boot:
1. Marks the readiness probe as DOWN — Kubernetes stops routing new traffic immediately
2. Lets in-flight requests complete (up to 30 s)
3. Closes the Tomcat acceptor and then shuts down the JVM

### Kubernetes coordination

```yaml
lifecycle:
  preStop:
    exec:
      command: ["/bin/sh", "-c", "sleep 5"]   # wait for load balancer to deregister

terminationGracePeriodSeconds: 60             # > 30s drain + 5s preStop
```

Timeline when Kubernetes terminates a pod:

```
t=0   SIGTERM sent; preStop sleep starts
t=5   Spring receives SIGTERM; stops accepting new requests; readiness → DOWN
t=35  All in-flight requests drained (30s max)
t=35  JVM exits
t=60  Kubernetes force-kills if JVM hasn't exited
```

---

## Health probes

```properties
management.endpoint.health.probes.enabled=true
management.health.livenessState.enabled=true
management.health.readinessState.enabled=true
```

| Endpoint | Probe type | Fails when | Kubernetes action |
|---|---|---|---|
| `/actuator/health/liveness` | Liveness | JVM is deadlocked / unrecoverable | Restart the pod |
| `/actuator/health/readiness` | Readiness | App is starting or shutting down | Remove pod from LB |
| `/actuator/health` | Aggregate | Any component is DOWN | Informational |

Custom `AppHealthIndicator` contributes deployment metadata to the aggregate:

```json
{
  "status": "UP",
  "components": {
    "app": {
      "status": "UP",
      "details": { "environment": "prod", "version": "1.0.0", "region": "us-east-1" }
    }
  }
}
```

---

## Cloud deployment targets

### AWS ECS Fargate

Fargate runs containers without managing EC2 instances. Key config:

- **Image**: pushed to ECR, referenced in the task definition
- **Secrets**: fetched from Secrets Manager at task start via the `secrets` block
- **Health check**: `wget` calls `/actuator/health`; task is replaced if it fails 3× at 30s intervals
- **Logs**: `awslogs` driver streams stdout to CloudWatch Logs
- **stopTimeout: 35** — Fargate waits 35 s after SIGTERM before force-killing (must exceed drain timeout)

```bash
# Deploy a new image
aws ecs update-service \
  --cluster production \
  --service cloud-deployment-demo \
  --force-new-deployment
```

### GCP App Engine Flexible

App Engine manages the VM fleet. Key config in `app.yaml`:

- `readiness_check.path: /actuator/health/readiness` — traffic only routes to healthy instances
- `liveness_check.path: /actuator/health/liveness` — unhealthy instances are replaced
- `automatic_scaling.min_num_instances: 1` — no cold starts
- `target_utilization: 0.65` — scale out when CPU > 65%

```bash
gcloud app deploy deployment/gcp/app.yaml --project=MY_PROJECT
```

### Kubernetes (GKE / EKS)

The `deployment/k8s/deployment.yaml` covers:

- `readinessProbe` + `livenessProbe` pointing at actuator endpoints
- Resource `requests` and `limits` (prevent noisy-neighbour problems)
- `preStop` hook for graceful shutdown coordination
- `terminationGracePeriodSeconds: 60` — buffer over the drain timeout

---

## Tests

| Class | Factor | Tests |
|---|---|---|
| `DeploymentInfoTest` | III, X | 2 |
| `EnvironmentOverrideTest` | III | 1 |
| `HealthIndicatorTest` | — | 2 |
| `GracefulShutdownTest` | IX | 1 |

Run: `JAVA_HOME=/opt/homebrew/opt/openjdk@21 mvn test`
Result: **6/6 pass**

---

## Key decisions

| Decision | Reason |
|---|---|
| `@ConfigurationProperties` record over `@Value` fields | Record makes all config visible as a single type; `@Validated` catches missing props at startup, not at first call |
| `server.shutdown=graceful` default-on | Zero-downtime rolling deploys depend on it; forgetting it causes 5xx during pod restarts |
| Separate `application-staging.properties` and `application-prod.properties` | Environment differences are explicit and reviewable in git; no runtime logic branches on env name |
| `terminationGracePeriodSeconds` > drain timeout + preStop sleep | If grace period ≤ drain timeout, Kubernetes force-kills the pod before requests finish — silent data loss |
| Liveness vs readiness as separate probes | Mixing them causes a restart loop during normal startup; readiness DOWN during shutdown is correct, liveness DOWN means the pod is broken |
| `APP_DB_PASSWORD` via ECS secrets / K8s secretKeyRef, not environment block | Plaintext env vars appear in `docker inspect`, ECS describe-tasks, and process listings; secrets-manager references do not |
