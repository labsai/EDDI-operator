# EDDI Kubernetes Operator

[![CI](https://github.com/labsai/eddi-operator/actions/workflows/ci.yml/badge.svg)](https://github.com/labsai/eddi-operator/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A modern, **Red Hat-certifiable** Kubernetes Operator for the [EDDI v6](https://github.com/labsai/EDDI) conversational AI platform. Built with the [Java Operator SDK](https://javaoperatorsdk.io/) and [Quarkus](https://quarkus.io/), it provides full lifecycle management of the EDDI stack through a single Custom Resource.

## Features

- **One-command deployment** — A single `Eddi` custom resource deploys the entire EDDI stack
- **Multi-database support** — MongoDB or PostgreSQL (managed or external)
- **NATS JetStream messaging** — Optional managed or external messaging
- **Keycloak authentication** — Optional managed or external OIDC (dev/production modes)
- **Auto-generated secrets** — Keycloak admin, PostgreSQL, and Vault credentials auto-generated on first deploy
- **OpenShift & Kubernetes** — Auto-detects Route (OpenShift) or Ingress (K8s)
- **Manager UI** — Optional EDDI configuration UI deployment
- **Observability** — ServiceMonitor, Grafana Dashboard, PrometheusRule
- **Autoscaling** — HPA with CPU/memory targets
- **Pod Disruption Budgets** — Safe rolling updates
- **Network Policies** — Namespace-scoped traffic restrictions
- **Configurable images** — Override all infrastructure images for air-gapped / enterprise registries
- **CRD validation** — Invalid spec values produce clear `Failed` phase with error message
- **Backup & Restore** — CronJob-based database backup to PVC or S3 with retention
- **Pod Scheduling** — nodeSelector, tolerations, affinity, topologySpreadConstraints
- **Custom Labels/Annotations** — Pod-level labels and annotations for service meshes, cost allocation
- **PVC Cleanup** — Configurable retention policy (`Retain` or `Delete`) on CR deletion
- **Upgrade Detection** — Automatic `Upgrading` phase when `spec.version` changes

---

## Quick Start

### Minimal (MongoDB, defaults)

```yaml
apiVersion: eddi.labs.ai/v1beta1
kind: Eddi
metadata:
  name: my-eddi
  namespace: eddi
spec:
  version: "6.0.0"
  replicas: 1
  datastore:
    type: mongodb
    managed:
      enabled: true
```

```bash
kubectl apply -f my-eddi.yaml
```

### Production (PostgreSQL + NATS + Auth + TLS)

```yaml
apiVersion: eddi.labs.ai/v1beta1
kind: Eddi
metadata:
  name: eddi-prod
  namespace: eddi
spec:
  version: "6.0.0"
  replicas: 3
  datastore:
    type: postgres
    managed:
      enabled: true
      image:
        repository: my-registry.example.com/postgres
        tag: "16-alpine"
      storage:
        size: 50Gi
        storageClassName: gp3
  messaging:
    type: nats
    managed:
      enabled: true
  auth:
    enabled: true
    managed:
      enabled: true
      mode: production
      adminSecretRef: keycloak-admin-secret
  exposure:
    type: auto
    host: eddi.example.com
    tls:
      enabled: true
      secretRef: eddi-tls
  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 10
    targetCPU: 70
  podDisruptionBudget:
    enabled: true
    minAvailable: 2
  monitoring:
    serviceMonitor:
      enabled: true
    grafanaDashboard:
      enabled: true
    alerts:
      enabled: true
  networkPolicy: true
```

---

## How It Works

### Architecture

The operator uses the **Dependent Resource** pattern from the Java Operator SDK. A single `EddiReconciler` declaratively manages 28 Kubernetes resources through a `@Workflow` annotation:

```
Eddi CR
  └─ EddiReconciler (main reconciler)
       ├─ ServiceAccountDR          (always)
       ├─ ConfigMapDR               (always)
       ├─ VaultSecretDR             (when no masterKeySecretRef)
       ├─ MongoStatefulSetDR        (when datastore.type=mongodb + managed)
       ├─ MongoServiceDR            (when datastore.type=mongodb + managed)
       ├─ PostgresSecretDR          (when datastore.type=postgres + managed)
       ├─ PostgresStatefulSetDR     (when datastore.type=postgres + managed)
       ├─ PostgresServiceDR         (when datastore.type=postgres + managed)
       ├─ NatsStatefulSetDR         (when messaging.type=nats + managed)
       ├─ NatsServiceDR             (when messaging.type=nats + managed)
       ├─ KeycloakSecretDR          (when auth.enabled + managed + no adminSecretRef)
       ├─ KeycloakDeploymentDR      (when auth.enabled + managed)
       ├─ KeycloakServiceDR         (when auth.enabled + managed)
       ├─ EddiDeploymentDR          (after infra ready)
       ├─ EddiServiceDR             (after deployment)
       ├─ RouteDR                   (OpenShift auto-detected)
       ├─ IngressDR                 (vanilla K8s auto-detected)
       ├─ ManagerDeploymentDR       (when manager.enabled)
       ├─ ManagerServiceDR          (when manager.enabled)
       ├─ HpaDR                     (when autoscaling.enabled)
       ├─ PdbDR                     (when podDisruptionBudget.enabled)
       ├─ NetworkPolicyDR           (when networkPolicy=true)
       ├─ ServiceMonitorDR          (when monitoring.serviceMonitor.enabled)
       ├─ GrafanaDashboardDR        (when monitoring.grafanaDashboard.enabled)
       ├─ PrometheusRuleDR          (when monitoring.alerts.enabled)
       ├─ BackupPvcDR               (when backup.enabled + storage.type=pvc)
       └─ BackupCronJobDR           (when backup.enabled)
```

### Reconciliation Flow

1. **Spec Validation** — Before any resources are created, the reconciler validates enum fields (`datastore.type`, `messaging.type`, `exposure.type`) and constraints (`replicas >= 1`). Invalid specs produce a `Failed` phase with a clear error message.

2. **Activation Conditions** — Each optional resource has a condition class that checks the `EddiSpec` to decide if the resource should be created. Conditions use `HasMetadata` generics for type-safe sharing across resource types (StatefulSet, Service, Secret).

3. **Dependency Ordering** — The `@Workflow` annotation's `dependsOn` parameter ensures correct ordering:
   - Infrastructure (MongoDB/PostgreSQL/NATS) deploys first
   - `readyPostcondition` on StatefulSets ensures infrastructure is ready before EDDI starts
   - EDDI Deployment waits for ConfigMap + VaultSecret + all infrastructure services
   - Ingress/Route deploy after EDDI Service

4. **Status Computation** — After each reconciliation:
   - `StatusUpdater` queries child resource states via the K8s API
   - Computes 5 conditions: `Available`, `DatastoreReady`, `MessagingReady`, `Progressing`, `Degraded`
   - `lastTransitionTime` is preserved when condition status hasn't changed (Kubernetes convention)
   - Sets `observedGeneration` for sync-state detection (used by ArgoCD, etc.)
   - Resolves the external URL from Route or Ingress
   - Computes phase: `Pending` → `Deploying` → `Running` (or `Failed`)

5. **Config Rollouts** — The ConfigMap data is SHA-256 hashed and injected as a pod annotation (`eddi.labs.ai/config-hash`). When config changes, the hash changes, forcing a Deployment rollout.

6. **Secret Management** — Secrets (Vault master key, PostgreSQL credentials, Keycloak admin) are auto-generated on first deployment and **preserved on subsequent reconciliations** to prevent data loss. The operator uses direct K8s client lookups (not `getSecondaryResource()`) to avoid ambiguity when multiple Secret DRs exist.

### Liveness & Readiness

All infrastructure components have both readiness and liveness probes:

| Component | Readiness | Liveness |
|-----------|-----------|----------|
| MongoDB | `mongosh --eval db.adminCommand('ping')` | Same (30s delay) |
| PostgreSQL | `pg_isready -U eddi` | Same (30s delay) |
| NATS | HTTP `/healthz:8222` | Same (15s delay) |
| Keycloak | HTTP `/health/ready:8080` | HTTP `/health/live:8080` (60s delay) |

### OpenShift Detection

The operator auto-detects whether it's running on OpenShift by checking for the `routes.route.openshift.io` CRD. This check is **cached** (per condition instance) to avoid API server churn. When `spec.exposure.type=auto`:
- **OpenShift** → creates a Route with edge TLS termination
- **Vanilla K8s** → creates an Ingress

### Package Structure

```
ai.labs.eddi.operator
├── crd/                     # Custom Resource Definition
│   ├── EddiResource.java    # Root CRD (eddi.labs.ai/v1beta1)
│   ├── EddiSpec.java        # Spec with all sub-specs
│   ├── EddiStatus.java      # Status subresource
│   └── spec/                # Sub-spec classes (15 files)
├── reconciler/
│   ├── EddiReconciler.java  # Main reconciler with @Workflow
│   └── StatusUpdater.java   # Status computation
├── dependent/
│   ├── core/                # Always-active DRs (5 files)
│   ├── datastore/           # MongoDB + PostgreSQL DRs (6 files)
│   ├── messaging/           # NATS DRs (2 files)
│   ├── auth/                # Keycloak DRs (3 files)
│   ├── exposure/            # Ingress + Route DRs (2 files)
│   ├── extras/              # Manager, HPA, PDB, NetworkPolicy (5 files)
│   └── monitoring/          # ServiceMonitor, PrometheusRule, Grafana (4 files)
├── conditions/              # Activation + Ready conditions (20 files)
└── util/                    # Labels, Hashing, Defaults, OpenShiftDetector (4 files)
```

---

## CRD Reference

### `spec` fields

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `version` | string | `"6.0.0"` | EDDI version (container image tag) |
| `replicas` | int | `1` | Number of EDDI server replicas |
| `image.repository` | string | `"labsai/eddi"` | Container image repository |
| `image.tag` | string | _(version)_ | Image tag override |
| `image.pullPolicy` | string | `"IfNotPresent"` | Image pull policy |
| `image.pullSecrets` | list | `[]` | Image pull secret names |
| `datastore.type` | string | `"mongodb"` | `"mongodb"` or `"postgres"` |
| `datastore.managed.enabled` | bool | `true` | Deploy managed database |
| `datastore.managed.image.repository` | string | `""` | Image override (default: `mongo` or `postgres`) |
| `datastore.managed.image.tag` | string | `""` | Tag override (default: `7.0` or `16-alpine`) |
| `datastore.managed.storage.size` | string | `"20Gi"` | PVC storage size |
| `datastore.managed.storage.storageClassName` | string | `""` | StorageClass (cluster default) |
| `datastore.external.connectionString` | string | `""` | External MongoDB connection string |
| `datastore.external.secretRef` | string | `""` | Secret with external DB credentials |
| `messaging.type` | string | `"in-memory"` | `"in-memory"` or `"nats"` |
| `messaging.managed.enabled` | bool | `true` | Deploy managed NATS |
| `messaging.managed.image.repository` | string | `"nats"` | NATS image repository |
| `messaging.managed.image.tag` | string | `"2.10-alpine"` | NATS image tag |
| `auth.enabled` | bool | `false` | Enable OIDC authentication |
| `auth.managed.enabled` | bool | `false` | Deploy managed Keycloak |
| `auth.managed.mode` | string | `"dev"` | `"dev"` (start-dev) or `"production"` (start) |
| `auth.managed.adminSecretRef` | string | `""` | Secret with Keycloak admin password (auto-generated if empty) |
| `auth.managed.image.repository` | string | `"quay.io/keycloak/keycloak"` | Keycloak image |
| `auth.managed.image.tag` | string | `"26.0"` | Keycloak version |
| `auth.external.authServerUrl` | string | `""` | External OIDC auth server URL |
| `auth.external.clientId` | string | `"eddi-backend"` | OIDC client ID |
| `exposure.type` | string | `"auto"` | `"auto"`, `"route"`, `"ingress"`, or `"none"` |
| `exposure.host` | string | `""` | Hostname for Route/Ingress |
| `exposure.tls.enabled` | bool | `true` | Enable TLS |
| `exposure.tls.secretRef` | string | `""` | TLS certificate secret |
| `exposure.ingressClassName` | string | `""` | Ingress class name |
| `exposure.annotations` | map | `{}` | Route/Ingress annotations |
| `manager.enabled` | bool | `false` | Deploy EDDI Manager UI |
| `monitoring.serviceMonitor.enabled` | bool | `false` | Create ServiceMonitor |
| `monitoring.grafanaDashboard.enabled` | bool | `false` | Create Grafana Dashboard ConfigMap |
| `monitoring.alerts.enabled` | bool | `false` | Create PrometheusRule |
| `autoscaling.enabled` | bool | `false` | Enable HPA |
| `autoscaling.minReplicas` | int | `2` | HPA minimum replicas |
| `autoscaling.maxReplicas` | int | `10` | HPA maximum replicas |
| `autoscaling.targetCPU` | int | `70` | Target CPU utilization % |
| `autoscaling.targetMemory` | int | `80` | Target memory utilization % |
| `podDisruptionBudget.enabled` | bool | `false` | Create PDB |
| `podDisruptionBudget.minAvailable` | int | `1` | Minimum available pods |
| `networkPolicy` | bool | `false` | Create NetworkPolicy |
| `vault.masterKeySecretRef` | string | `""` | External vault key secret (auto-generated if empty) |
| `scheduling.nodeSelector` | map | `{}` | Node label selector for pod placement |
| `scheduling.tolerations` | list | `[]` | Tolerations for tainted nodes |
| `scheduling.affinity` | object | `null` | Affinity/anti-affinity rules |
| `scheduling.topologySpreadConstraints` | list | `[]` | Zone-awareness constraints |
| `podLabels` | map | `{}` | Additional labels applied to EDDI server pods |
| `podAnnotations` | map | `{}` | Additional annotations (e.g., `sidecar.istio.io/inject`) |
| `pvcRetentionPolicy` | string | `"Retain"` | PVC cleanup on CR deletion: `Retain` or `Delete` |
| `backup.enabled` | bool | `false` | Enable scheduled database backups |
| `backup.schedule` | string | `"0 2 * * *"` | Backup cron schedule |
| `backup.retentionDays` | int | `7` | Delete backups older than N days |
| `backup.storage.type` | string | `"pvc"` | Backup target: `pvc` or `s3` |
| `backup.storage.pvc.size` | string | `"50Gi"` | Backup PVC size |
| `backup.storage.s3.bucket` | string | `""` | S3 bucket name |
| `backup.storage.s3.secretRef` | string | `""` | Secret with S3 access/secret keys |
| `resources.requests.cpu` | string | `"250m"` | EDDI CPU request |
| `resources.requests.memory` | string | `"384Mi"` | EDDI memory request |
| `resources.limits.cpu` | string | `"2"` | EDDI CPU limit |
| `resources.limits.memory` | string | `"1Gi"` | EDDI memory limit |
| `cors` | string | `"http://localhost:3000,..."` | CORS allowed origins |

### `status` fields

| Field | Description |
|-------|-------------|
| `phase` | `Pending`, `Deploying`, `Running`, `Failed`, `Upgrading` |
| `version` | Currently targeted version |
| `replicas` | Desired replica count |
| `readyReplicas` | Ready replica count |
| `url` | Resolved external URL |
| `observedGeneration` | Last reconciled CR generation |
| `conditions[]` | Kubernetes-standard conditions array |

---

## Technology Stack

| Component | Version |
|---|---|
| Java | 21 (LTS) |
| Quarkus | 3.34.x |
| Java Operator SDK (JOSDK) | 5.3.x (via QOSDK 7.7.0) |
| Fabric8 Kubernetes Client | 7.x |

## Building

```bash
# Compile
mvn compile

# Run tests
mvn test

# Package (JVM)
mvn package

# Build native image
mvn package -Pnative

# Build Docker image (JVM)
docker build -f Dockerfile.jvm -t eddi-operator:6.0.0 .

# Build Docker image (Native)
docker build -f Dockerfile.native -t eddi-operator:6.0.0-native .
```

## Testing

The operator has a comprehensive, three-tier test suite with **329+ tests**.

```bash
# Run all tests (unit + integration)
mvn test

# Run unit tests only
mvn test -Dtest="ai.labs.eddi.operator.unit.*"

# Run integration tests only (uses Quarkus mock K8s server)
mvn test -Dtest="ai.labs.eddi.operator.integration.*"

# Convenience (via Makefile)
make test          # all tests
make test-unit     # unit only
make test-integration  # integration only
```

### Test Architecture

| Tier | Tests | Runner | What It Validates |
|------|-------|--------|-------------------|
| **Unit** | ~310 | JUnit 5 + AssertJ | DR `desired()` output, activation conditions, spec validation, utility functions |
| **Integration** | ~10 | `@QuarkusTest` + mock K8s server | Full reconciliation workflows, status computation, resource creation |
| **E2E** _(scaffold)_ | 2 | Testcontainers K3s | CRD install, real-cluster lifecycle (Linux-only, `@Disabled` until CI image pipeline) |

### Unit Test Coverage

| Test Suite | Tests | Coverage |
|-----------|-------|----------|
| `DatastoreDRTest` | 22 | Mongo/Postgres StatefulSets, Services — image, ports, PVC, probes, security |
| `MessagingDRTest` | 10 | NATS StatefulSet + Service — JetStream args, dual ports, health probes |
| `ExposureDRTest` | 15 | Ingress + Route — TLS defaults, host routing, backends, ingressClassName |
| `AuthDRTest` | 12 | Keycloak Deployment (dev/prod mode), Service, credential generation |
| `MonitoringDRTest` | 13 | ServiceMonitor, GrafanaDashboard, PrometheusRule (PromQL correctness) |
| `ManagerDRTest` | 9 | Manager Deployment (API URL wiring), Service |
| `CoreDRTest` | 24 | ServiceAccount, EddiService, VaultSecret, HPA, PDB, NetworkPolicy, BackupPvc |
| `ActivationConditionTests` | 42 | All 20 activation conditions (positive + negative) |
| `ReconcilerValidationTest` | 17 | Spec validation edge cases (replicas, cron, S3 buckets) |
| `ConfigMapDRTest` | 8 | ConfigMap data for all datastore/messaging combinations |
| `EddiDeploymentDRTest` | 11 | Deployment DR helpers, image resolution |
| `ConditionTests` | 10 | Activation conditions, `Defaults.*` utilities |
| `EnterpriseHardeningTests` | 23 | Scheduling, labels, PVC retention, backup validation |
| `ValidationAndSecurityTests` | 32 | Spec validation, image resolution, shell sanitization |
| `BackupCronJobDRTest` | 10 | Backup command generation, retention, image resolution |
| `ShellSanitizationTest` | 16 | Input sanitization for shell commands |
| `SecretResilienceTest` | 21 | Secret lookup failure handling |
| `UtilTests` | 7 | Labels, Hashing |
| `ExtrasDRTest` | 13 | Default values for CRD spec fields |

## Documentation

- [User Guide](docs/user-guide.md)
- [RBAC & Multi-Tenancy](docs/rbac.md)
- [Operator Plan](docs/OPERATOR_V2_PLAN.md)

## License

Apache License 2.0 — see [LICENSE](LICENSE).
