# Changelog

All notable changes to the EDDI Kubernetes Operator will be documented in this file.

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [6.0.0] — 2026-04-09

### Initial Release

First production release of the EDDI Kubernetes Operator for EDDI v6.

### Features

- **Full-Stack Deployment** — Single `Eddi` CR deploys the entire EDDI v6 stack
- **Multi-Database Support** — MongoDB or PostgreSQL (managed or external)
- **NATS Messaging** — Optional managed or external NATS JetStream
- **Keycloak Authentication** — Optional managed or external OIDC (dev/production modes)
- **Auto-Generated Secrets** — Vault master key, PostgreSQL credentials, and Keycloak admin passwords auto-generated securely on first deployment
- **OpenShift & Kubernetes** — Auto-detects Route (OpenShift) or Ingress (K8s)
- **Manager UI** — Optional EDDI configuration UI deployment
- **Configurable Images** — Override all infrastructure images for air-gapped / enterprise registries
- **CRD Validation** — Invalid spec values produce clear `Failed` phase with error message
- **Liveness Probes** — All infrastructure components have health checks (Mongo, Postgres, NATS, Keycloak)
- **Pod Security Standards** — All containers run with `restricted` security context profile
- **Observability** — ServiceMonitor, Grafana Dashboard, PrometheusRule integration
- **Autoscaling** — HPA with CPU/memory targets
- **Pod Disruption Budgets** — Safe rolling updates
- **Network Policies** — Namespace-scoped traffic restrictions
- **Kubernetes Events** — Reconciliation lifecycle events visible via `kubectl describe`
- **Drift Correction** — Automatic re-reconciliation every 5 minutes via `@MaxReconciliationInterval`
- **Finalizer** — Graceful cleanup on CR deletion with event recording
- **Backup & Restore** — CronJob-based database backup to PVC or S3 with configurable retention
- **Pod Scheduling** — `nodeSelector`, `tolerations`, `affinity`, and `topologySpreadConstraints`
- **Custom Pod Labels & Annotations** — For service mesh injection, cost allocation, and security scanning
- **PVC Retention Policy** — Configurable `Retain` or `Delete` on CR deletion
- **Upgrade Detection** — Automatic `Upgrading` phase when `spec.version` changes
- **RBAC Documentation** — Minimum ClusterRole, namespace-scoped mode, multi-tenancy guide

### Architecture

- Built on **Java 21**, **Quarkus 3.34.x**, **JOSDK 5.3.x**
- 28 Dependent Resources managed declaratively via `@Workflow`
- 21 Activation/Ready conditions for fine-grained resource control
- Red Hat-certifiable with `@Plural`, securityContext, and UBI 9 base images

### Testing

- 85+ unit tests covering conditions, DRs, validation, security, enterprise hardening, and utilities
- Integration tests with `@QuarkusTest` + `MockKubernetesServer`
- CI pipeline with unit tests, integration tests, JVM and native image builds, OLM bundle validation
