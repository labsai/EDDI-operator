# EDDI Operator — RBAC Requirements

## Overview

The EDDI Operator requires cluster-level permissions to manage Custom Resource Definitions and watch `Eddi` CRs across namespaces. The Quarkus Operator SDK (QOSDK) auto-generates the required RBAC manifests at build time in `target/kubernetes/`.

## Minimum Required ClusterRole

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: ClusterRole
metadata:
  name: eddi-operator
rules:
  # Core Eddi CRD management
  - apiGroups: ["eddi.labs.ai"]
    resources: ["eddis", "eddis/status", "eddis/finalizers"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  # Core Kubernetes resources managed by the operator
  - apiGroups: [""]
    resources: ["configmaps", "secrets", "services", "serviceaccounts", "persistentvolumeclaims", "events"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  - apiGroups: ["apps"]
    resources: ["deployments", "statefulsets"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  # Network exposure
  - apiGroups: ["networking.k8s.io"]
    resources: ["ingresses", "networkpolicies"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  # OpenShift Route (optional — only needed on OpenShift)
  - apiGroups: ["route.openshift.io"]
    resources: ["routes"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  # Auto-detection of OpenShift (read-only)
  - apiGroups: ["apiextensions.k8s.io"]
    resources: ["customresourcedefinitions"]
    verbs: ["get", "list"]

  # Autoscaling
  - apiGroups: ["autoscaling"]
    resources: ["horizontalpodautoscalers"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  # Pod Disruption Budgets
  - apiGroups: ["policy"]
    resources: ["poddisruptionbudgets"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  # Backup CronJobs
  - apiGroups: ["batch"]
    resources: ["cronjobs", "jobs"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  # Monitoring (optional — only if Prometheus/Grafana Operators are installed)
  - apiGroups: ["monitoring.coreos.com"]
    resources: ["servicemonitors", "prometheusrules"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  - apiGroups: ["integreatly.org"]
    resources: ["grafanadashboards"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]

  # Leader election (operator HA)
  - apiGroups: ["coordination.k8s.io"]
    resources: ["leases"]
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
```

## Namespace-Scoped Mode

For operators that should be restricted to a single namespace, set:

```properties
# application.properties
quarkus.operator-sdk.namespaces=my-namespace
```

In this mode, use a `Role` + `RoleBinding` instead of `ClusterRole` + `ClusterRoleBinding` (omit the `apiextensions.k8s.io` rule, which always requires cluster scope).

## Multi-Tenancy

The operator supports multiple `Eddi` CRs in different namespaces. Each CR is independent — all child resources are scoped to the CR's namespace and prefixed with the CR name (e.g., `my-eddi-server`, `my-eddi-mongodb`).

### Running Multiple Instances

```yaml
# Namespace: eddi-dev
apiVersion: eddi.labs.ai/v1beta1
kind: Eddi
metadata:
  name: dev-eddi
  namespace: eddi-dev
spec:
  version: "6.0.0"
  replicas: 1

---
# Namespace: eddi-prod
apiVersion: eddi.labs.ai/v1beta1
kind: Eddi
metadata:
  name: prod-eddi
  namespace: eddi-prod
spec:
  version: "6.0.0"
  replicas: 3
```

Both instances are fully independent — separate databases, separate services, separate secrets.

### Best Practices

1. **One namespace per environment** — Isolate dev/staging/prod in separate namespaces
2. **Unique CR names** — Each `Eddi` CR must have a unique name within its namespace
3. **NetworkPolicy** — Enable `spec.networkPolicy: true` to restrict cross-namespace traffic
4. **RBAC** — Use namespace-scoped Roles for least-privilege when the operator only manages a single namespace

## Service Account

The operator runs under a dedicated ServiceAccount. For each `Eddi` CR, a separate ServiceAccount is created for the EDDI server pods:

```
eddi-operator        → Operator pod (cluster-scoped permissions)
<cr-name>-server     → EDDI server pods (namespace-scoped, minimal permissions)
```

## Auditing

All operator actions emit Kubernetes Events visible via:

```bash
kubectl describe eddi <name>
kubectl get events --field-selector involvedObject.kind=Eddi
```

Event types: `ReconcileSucceeded`, `ReconcileFailed`, `ReconcileError`, `SpecInvalid`, `CleanupStarted`.
