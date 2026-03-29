# EDDI Kubernetes Operator

[![CI](https://github.com/labsai/eddi-operator/actions/workflows/ci.yml/badge.svg)](https://github.com/labsai/eddi-operator/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

A modern, **Red Hat-certifiable** Kubernetes Operator for the [EDDI v6](https://github.com/labsai/EDDI) conversational AI platform. Built with the [Java Operator SDK](https://javaoperatorsdk.io/) and [Quarkus](https://quarkus.io/), it provides full lifecycle management of the EDDI stack through a single Custom Resource.

## Features

- **One-command deployment** — A single `Eddi` custom resource deploys the entire EDDI stack
- **Multi-database support** — MongoDB or PostgreSQL (managed or external)
- **NATS JetStream messaging** — Optional managed or external messaging
- **Keycloak authentication** — Optional managed or external OIDC (dev/production modes)
- **OpenShift & Kubernetes** — Auto-detects Route (OpenShift) or Ingress (K8s)
- **Manager UI** — Optional EDDI configuration UI deployment
- **Observability** — ServiceMonitor, Grafana Dashboard, PrometheusRule
- **Autoscaling** — HPA with CPU/memory targets
- **Backup & Restore** — CronJob-based database backup to PVC or S3
- **Pod Disruption Budgets** — Safe rolling updates
- **Network Policies** — Namespace-scoped traffic restrictions

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

The operator uses the **Dependent Resource** pattern from the Java Operator SDK. A single `EddiReconciler` declaratively manages 20+ Kubernetes resources through a `@Workflow` annotation:

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
       └─ NetworkPolicyDR           (when networkPolicy=true)
```

### Reconciliation Flow

1. **Activation Conditions** — Each optional resource has a condition class that checks the `EddiSpec` to decide if the resource should be created. Conditions use `HasMetadata` generics for type-safe sharing across resource types (StatefulSet, Service, Secret).

2. **Dependency Ordering** — The `@Workflow` annotation's `dependsOn` parameter ensures correct ordering:
   - Infrastructure (MongoDB/PostgreSQL/NATS) deploys first
   - `readyPostcondition` on StatefulSets ensures infrastructure is ready before EDDI starts
   - EDDI Deployment waits for ConfigMap + VaultSecret + all infrastructure services
   - Ingress/Route deploy after EDDI Service

3. **Status Computation** — After each reconciliation:
   - `StatusUpdater` queries child resource states via the K8s API
   - Computes 5 conditions: `Available`, `DatastoreReady`, `MessagingReady`, `Progressing`, `Degraded`
   - `lastTransitionTime` is preserved when condition status hasn't changed (Kubernetes convention)
   - Sets `observedGeneration` for sync-state detection (used by ArgoCD, etc.)
   - Resolves the external URL from Route or Ingress
   - Computes phase: `Pending` → `Deploying` → `Running` (or `Failed`)

4. **Config Rollouts** — The ConfigMap data is SHA-256 hashed and injected as a pod annotation (`eddi.labs.ai/config-hash`). When config changes, the hash changes, forcing a Deployment rollout.

5. **Secret Management** — Secrets (Vault master key, PostgreSQL credentials) are auto-generated on first deployment and **preserved on subsequent reconciliations** to prevent data loss. The operator uses direct K8s client lookups (not `getSecondaryResource()`) to avoid ambiguity when multiple Secret DRs exist.

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
│   └── spec/                # Sub-spec classes (14 files)
├── reconciler/
│   ├── EddiReconciler.java  # Main reconciler with @Workflow
│   └── StatusUpdater.java   # Status computation
├── dependent/
│   ├── core/                # Always-active DRs (5 files)
│   ├── datastore/           # MongoDB + PostgreSQL DRs (5 files)
│   ├── messaging/           # NATS DRs (2 files)
│   ├── auth/                # Keycloak DRs (2 files)
│   ├── exposure/            # Ingress + Route DRs (2 files)
│   ├── extras/              # Manager, HPA, PDB, NetworkPolicy (5 files)
│   └── monitoring/          # ServiceMonitor, PrometheusRule, Grafana (4 files)
├── conditions/              # Activation + Ready conditions (16 files)
└── util/                    # Labels, Hashing, Defaults (3 files)
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
| `datastore.managed.storage.size` | string | `"20Gi"` | PVC storage size |
| `datastore.managed.storage.storageClassName` | string | `""` | StorageClass (default = cluster default) |
| `datastore.external.connectionString` | string | `""` | External MongoDB connection string |
| `datastore.external.secretRef` | string | `""` | Secret with external DB credentials |
| `messaging.type` | string | `"in-memory"` | `"in-memory"` or `"nats"` |
| `messaging.managed.enabled` | bool | `true` | Deploy managed NATS |
| `auth.enabled` | bool | `false` | Enable OIDC authentication |
| `auth.managed.enabled` | bool | `false` | Deploy managed Keycloak |
| `auth.managed.mode` | string | `"dev"` | `"dev"` (start-dev) or `"production"` (start) |
| `auth.managed.adminSecretRef` | string | `""` | Secret with Keycloak admin password |
| `auth.external.authServerUrl` | string | `""` | External OIDC auth server URL |
| `auth.external.clientId` | string | `"eddi-backend"` | OIDC client ID |
| `exposure.type` | string | `"auto"` | `"auto"`, `"route"`, `"ingress"`, or `"none"` |
| `exposure.host` | string | `""` | Hostname for Route/Ingress |
| `exposure.tls.enabled` | bool | `true` | Enable TLS |
| `exposure.tls.secretRef` | string | `""` | TLS certificate secret |
| `exposure.ingressClassName` | string | `""` | Ingress class name |
| `exposure.annotations` | map | `{}` | Route/Ingress annotations |
| `manager.enabled` | bool | `true` | Deploy EDDI Manager UI |
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
./mvnw compile

# Run tests
./mvnw test

# Package (JVM)
./mvnw package

# Build native image
./mvnw package -Pnative

# Build Docker image (JVM)
docker build -f Dockerfile.jvm -t eddi-operator:latest .

# Build Docker image (Native)
docker build -f Dockerfile.native -t eddi-operator:latest-native .
```

## Documentation

- [User Guide](docs/user-guide.md)
- [Operator Plan](docs/OPERATOR_V2_PLAN.md)

## License

Apache License 2.0 — see [LICENSE](LICENSE).
