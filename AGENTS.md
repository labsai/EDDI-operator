# AGENTS.md — AI Assistant Instructions for EDDI Operator

## Project Overview

This is a **Kubernetes Operator** for the EDDI v6 conversational AI platform. It is built with:

- **Java 21** (NOT Java 25 — operators must be conservative)
- **Quarkus 3.34.x** as the application framework
- **Java Operator SDK (JOSDK) 5.3.x** via the Quarkus Operator SDK extension (QOSDK)
- **Fabric8 Kubernetes Client 7.x** (managed transitively by Quarkus BOM)

## Architecture

The operator follows the **Dependent Resource** pattern from JOSDK:

1. **`EddiReconciler`** — The main reconciler, annotated with `@Workflow` listing all dependent resources
2. **Dependent Resources (DRs)** — Each Kubernetes resource the operator manages is a separate `CRUDKubernetesDependentResource` subclass
3. **Activation Conditions** — Each optional DR has a condition that checks the `EddiSpec` to determine if it should be active
4. **Ready Postconditions** — Infrastructure DRs (database, messaging) have readiness checks; the EDDI Deployment only deploys after infrastructure is ready

## Key Conventions

- **CRD API Group:** `eddi.labs.ai`, version `v1beta1`
- **Kind:** `Eddi`
- **Package structure:** `ai.labs.eddi.operator.{crd, reconciler, dependent, conditions, util}`
- **All child resources** get owner references to the `Eddi` CR for garbage collection
- **Labels** use the `app.kubernetes.io/` standard label set via `Labels.java`
- **ConfigMap hashing** — `Hashing.java` computes a hash of the ConfigMap data; this hash is added as a pod annotation to force rollouts on config change

## Do NOT

- Use Java 25 features (records with named patterns, etc.) — stick to Java 21
- Add dependencies not managed by the Quarkus BOM without explicit approval
- Change the API group or version without updating the plan document
- Deploy Keycloak realm configuration — managed Keycloak is server-only

## Testing Tiers

1. **Unit tests** (`src/test/java/.../unit/`) — test `desired()` output of DRs, condition logic
2. **Integration tests** (`src/test/java/.../integration/`) — use `@QuarkusTest` + `MockKubernetesServer`
3. **E2E tests** (`src/test/java/.../e2e/`) — use Testcontainers + K3s (future)
