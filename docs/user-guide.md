# EDDI Operator — User Guide

## Table of Contents

- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration Reference](#configuration-reference)
- [Deployment Examples](#deployment-examples)
- [Monitoring](#monitoring)
- [Backup & Restore](#backup--restore)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

- Kubernetes 1.27+ or OpenShift 4.14+
- `kubectl` or `oc` CLI
- Cluster-admin access for CRD installation

## Installation

### From OLM (OperatorHub)

```bash
# Create CatalogSource
kubectl apply -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: CatalogSource
metadata:
  name: eddi-operator-catalog
  namespace: olm
spec:
  sourceType: grpc
  image: quay.io/labsai/eddi-operator-catalog:latest
  displayName: EDDI Operator
  publisher: LABS.AI
EOF

# Subscribe to the operator
kubectl apply -f - <<EOF
apiVersion: operators.coreos.com/v1alpha1
kind: Subscription
metadata:
  name: eddi-operator
  namespace: operators
spec:
  channel: stable
  name: eddi-operator
  source: eddi-operator-catalog
  sourceNamespace: olm
EOF
```

### Direct Installation

```bash
# Install CRD
kubectl apply -f target/kubernetes/eddis.eddi.labs.ai-v1.yml

# Deploy operator
kubectl apply -f target/kubernetes/kubernetes.yml
```

---

## Configuration Reference

### Minimal Example

```yaml
apiVersion: eddi.labs.ai/v1beta1
kind: Eddi
metadata:
  name: my-eddi
spec:
  version: "6.0.0"
```

This deploys EDDI with:
- 1 replica
- Managed MongoDB (20Gi storage)
- In-memory messaging
- No authentication
- Auto-detected network exposure
- Manager UI enabled

### Full Example

```yaml
apiVersion: eddi.labs.ai/v1beta1
kind: Eddi
metadata:
  name: production-eddi
  namespace: eddi-production
spec:
  version: "6.0.0"
  replicas: 3

  image:
    repository: labsai/eddi
    pullPolicy: IfNotPresent
    pullSecrets:
      - my-registry-secret

  datastore:
    type: postgres
    managed:
      enabled: false
    external:
      secretRef: production-pg-credentials

  messaging:
    type: nats
    external:
      url: nats://nats.messaging:4222

  vault:
    masterKeySecretRef: eddi-vault-key

  auth:
    enabled: true
    external:
      authServerUrl: https://keycloak.example.com/realms/eddi
      clientId: eddi-backend

  exposure:
    type: ingress
    host: eddi.example.com
    tls:
      enabled: true
      secretRef: eddi-tls-cert
    ingressClassName: nginx

  manager:
    enabled: true

  monitoring:
    serviceMonitor:
      enabled: true
      interval: 15s
    grafanaDashboard:
      enabled: true
    alerts:
      enabled: true

  autoscaling:
    enabled: true
    minReplicas: 3
    maxReplicas: 10
    targetCPU: 70
    targetMemory: 80

  podDisruptionBudget:
    enabled: true
    minAvailable: 2

  resources:
    requests:
      cpu: 500m
      memory: 512Mi
    limits:
      cpu: "2"
      memory: 2Gi

  backup:
    enabled: true
    schedule: "0 2 * * *"
    retentionDays: 30
    storage:
      type: s3
      s3:
        bucket: eddi-backups
        region: eu-central-1
        secretRef: s3-backup-credentials

  cors:
    origins: "https://app.example.com"

  networkPolicy:
    enabled: true
```

### Spec Fields

| Field | Type | Default | Description |
|---|---|---|---|
| `spec.version` | string | `"6.0.0"` | EDDI version (image tag) |
| `spec.replicas` | int | `1` | Number of EDDI replicas |
| `spec.image.repository` | string | `"labsai/eddi"` | Container image repository |
| `spec.image.tag` | string | `""` | Image tag (empty = use version) |
| `spec.image.pullPolicy` | string | `"IfNotPresent"` | Image pull policy |
| `spec.datastore.type` | string | `"mongodb"` | `"mongodb"` or `"postgres"` |
| `spec.datastore.managed.enabled` | bool | `true` | Deploy managed database |
| `spec.datastore.external.connectionString` | string | `""` | External DB connection URI |
| `spec.datastore.external.secretRef` | string | `""` | Secret with DB credentials |
| `spec.messaging.type` | string | `"in-memory"` | `"in-memory"` or `"nats"` |
| `spec.vault.masterKeySecretRef` | string | `""` | Secret with vault master key |
| `spec.auth.enabled` | bool | `false` | Enable OIDC authentication |
| `spec.exposure.type` | string | `"auto"` | `"auto"`, `"route"`, `"ingress"`, `"none"` |
| `spec.manager.enabled` | bool | `true` | Deploy Manager UI |
| `spec.monitoring.serviceMonitor.enabled` | bool | `false` | Create ServiceMonitor |
| `spec.autoscaling.enabled` | bool | `false` | Enable HPA |
| `spec.backup.enabled` | bool | `false` | Enable automated backups |

### Status Fields

| Field | Description |
|---|---|
| `status.phase` | `Pending`, `Deploying`, `Running`, `Failed`, `Upgrading` |
| `status.version` | Currently running version |
| `status.replicas` | Desired replica count |
| `status.readyReplicas` | Number of ready replicas |
| `status.url` | External URL (from Route/Ingress) |
| `status.conditions` | Kubernetes-standard conditions array |

### Status Conditions

| Condition | Description |
|---|---|
| `Available` | At least one replica is ready |
| `DatastoreReady` | Database is connected and healthy |
| `MessagingReady` | NATS is connected (if configured) |
| `Progressing` | Deployment is rolling out |
| `Degraded` | System is up but not all replicas are ready |

---

## Deployment Examples

### Development (Everything Managed)

```yaml
apiVersion: eddi.labs.ai/v1beta1
kind: Eddi
metadata:
  name: dev-eddi
spec:
  version: "6.0.0"
  datastore:
    type: mongodb
    managed:
      enabled: true
      storage:
        size: 5Gi
```

### Production (External Services)

```yaml
apiVersion: eddi.labs.ai/v1beta1
kind: Eddi
metadata:
  name: prod-eddi
spec:
  version: "6.0.0"
  replicas: 3
  datastore:
    type: postgres
    managed:
      enabled: false
    external:
      secretRef: cloudnativepg-credentials
  messaging:
    type: nats
    external:
      url: nats://nats-operator.nats:4222
```

---

## Monitoring

When `monitoring.serviceMonitor.enabled: true`, the operator creates a ServiceMonitor that scrapes EDDI's `/q/metrics` Prometheus endpoint.

### Available Metrics

- `http_server_requests_seconds_*` — Request latency histograms
- `jvm_memory_*` — JVM memory usage
- `eddi_conversations_active` — Active conversation count
- Standard Micrometer/Quarkus metrics

### Default Alerts

When `monitoring.alerts.enabled: true`:
- **EddiDown** — Pod has been unreachable for 5+ minutes
- **EddiHighErrorRate** — >5% of requests returning 5xx for 10+ minutes
- **EddiHighLatency** — P99 latency above 2 seconds for 10+ minutes

---

## Backup & Restore

### Enable Backups

```yaml
spec:
  backup:
    enabled: true
    schedule: "0 2 * * *"    # Daily at 2 AM
    retentionDays: 7
    storage:
      type: pvc
      pvc:
        size: 50Gi
```

### Trigger Restore

Annotate the CR to trigger a restore:

```bash
kubectl annotate eddi my-eddi eddi.labs.ai/restore-from=backup-2026-03-29 --overwrite
```

---

## Troubleshooting

### Check Operator Logs

```bash
kubectl logs -l app.kubernetes.io/name=eddi-operator -f
```

### Check CR Status

```bash
kubectl get eddi my-eddi -o yaml
```

### Check Conditions

```bash
kubectl get eddi my-eddi -o jsonpath='{.status.conditions}' | jq .
```

### Common Issues

| Issue | Cause | Resolution |
|---|---|---|
| Phase: Pending | Database not ready | Check managed DB StatefulSet, or verify external connection |
| Phase: Failed | Reconciliation error | Check operator logs for stack traces |
| No URL in status | No Route/Ingress | Verify exposure.type and host settings |
| Pods not starting | Image pull error | Check image repository, tag, and pullSecrets |
