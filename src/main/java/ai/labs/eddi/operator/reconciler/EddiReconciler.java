package ai.labs.eddi.operator.reconciler;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiStatus;

import ai.labs.eddi.operator.dependent.core.*;
import ai.labs.eddi.operator.dependent.datastore.*;
import ai.labs.eddi.operator.dependent.messaging.*;
import ai.labs.eddi.operator.dependent.auth.*;
import ai.labs.eddi.operator.dependent.exposure.*;
import ai.labs.eddi.operator.dependent.extras.*;

import ai.labs.eddi.operator.conditions.*;

import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

import org.jboss.logging.Logger;

/**
 * Main reconciler for the Eddi custom resource.
 * Uses the @Workflow annotation to declare all dependent resources and their relationships.
 *
 * The workflow manages 20+ Kubernetes resources with activation conditions,
 * readiness postconditions, and correct deployment ordering.
 */
@ControllerConfiguration
@Workflow(dependents = {
        // ── Always Active: Core ──────────────────────────────
        @Dependent(
                name = "service-account",
                type = ServiceAccountDR.class
        ),
        @Dependent(
                name = "config-map",
                type = ConfigMapDR.class,
                dependsOn = {"service-account"}
        ),
        @Dependent(
                name = "vault-secret",
                type = VaultSecretDR.class,
                activationCondition = VaultSecretActivationCondition.class,
                dependsOn = {"service-account"}
        ),

        // ── Conditional: MongoDB ─────────────────────────────
        @Dependent(
                name = "mongo-statefulset",
                type = MongoStatefulSetDR.class,
                activationCondition = MongoActivationCondition.class,
                readyPostcondition = MongoReadyCondition.class
        ),
        @Dependent(
                name = "mongo-service",
                type = MongoServiceDR.class,
                activationCondition = MongoActivationCondition.class,
                dependsOn = {"mongo-statefulset"}
        ),

        // ── Conditional: PostgreSQL ──────────────────────────
        @Dependent(
                name = "postgres-secret",
                type = PostgresSecretDR.class,
                activationCondition = PostgresActivationCondition.class
        ),
        @Dependent(
                name = "postgres-statefulset",
                type = PostgresStatefulSetDR.class,
                activationCondition = PostgresActivationCondition.class,
                readyPostcondition = PostgresReadyCondition.class,
                dependsOn = {"postgres-secret"}
        ),
        @Dependent(
                name = "postgres-service",
                type = PostgresServiceDR.class,
                activationCondition = PostgresActivationCondition.class,
                dependsOn = {"postgres-statefulset"}
        ),

        // ── Conditional: NATS ────────────────────────────────
        @Dependent(
                name = "nats-statefulset",
                type = NatsStatefulSetDR.class,
                activationCondition = NatsActivationCondition.class,
                readyPostcondition = NatsReadyCondition.class
        ),
        @Dependent(
                name = "nats-service",
                type = NatsServiceDR.class,
                activationCondition = NatsActivationCondition.class,
                dependsOn = {"nats-statefulset"}
        ),

        // ── Conditional: Auth (Keycloak) ─────────────────────
        @Dependent(
                name = "keycloak-deployment",
                type = KeycloakDeploymentDR.class,
                activationCondition = ManagedAuthActivationCondition.class
        ),
        @Dependent(
                name = "keycloak-service",
                type = KeycloakServiceDR.class,
                activationCondition = ManagedAuthActivationCondition.class,
                dependsOn = {"keycloak-deployment"}
        ),

        // ── Core: EDDI Server (depends on infra readiness) ──
        @Dependent(
                name = "eddi-deployment",
                type = EddiDeploymentDR.class,
                dependsOn = {"config-map", "vault-secret",
                        "mongo-service", "postgres-service", "nats-service"}
        ),
        @Dependent(
                name = "eddi-service",
                type = EddiServiceDR.class,
                dependsOn = {"eddi-deployment"}
        ),

        // ── Conditional: Exposure ────────────────────────────
        @Dependent(
                name = "route",
                type = RouteDR.class,
                activationCondition = RouteActivationCondition.class,
                dependsOn = {"eddi-service"}
        ),
        @Dependent(
                name = "ingress",
                type = IngressDR.class,
                activationCondition = IngressActivationCondition.class,
                dependsOn = {"eddi-service"}
        ),

        // ── Conditional: Manager UI ──────────────────────────
        @Dependent(
                name = "manager-deployment",
                type = ManagerDeploymentDR.class,
                activationCondition = ManagerActivationCondition.class,
                dependsOn = {"eddi-service"}
        ),
        @Dependent(
                name = "manager-service",
                type = ManagerServiceDR.class,
                activationCondition = ManagerActivationCondition.class,
                dependsOn = {"manager-deployment"}
        ),

        // ── Conditional: Extras (gated by spec flags) ────────
        @Dependent(
                name = "hpa",
                type = HpaDR.class,
                activationCondition = HpaActivationCondition.class,
                dependsOn = {"eddi-deployment"}
        ),
        @Dependent(
                name = "pdb",
                type = PdbDR.class,
                activationCondition = PdbActivationCondition.class,
                dependsOn = {"eddi-deployment"}
        ),
        @Dependent(
                name = "network-policy",
                type = NetworkPolicyDR.class,
                activationCondition = NetworkPolicyActivationCondition.class,
                dependsOn = {"eddi-deployment"}
        )
})
public class EddiReconciler implements Reconciler<EddiResource> {

    private static final Logger LOG = Logger.getLogger(EddiReconciler.class);

    private final StatusUpdater statusUpdater;

    public EddiReconciler(StatusUpdater statusUpdater) {
        this.statusUpdater = statusUpdater;
    }

    @Override
    public UpdateControl<EddiResource> reconcile(EddiResource eddi,
                                                   Context<EddiResource> context) {
        LOG.infof("Reconciling Eddi '%s' in namespace '%s'",
                eddi.getMetadata().getName(),
                eddi.getMetadata().getNamespace());

        // Status is computed from the state of child resources
        var status = statusUpdater.computeStatus(eddi, context);
        eddi.setStatus(status);

        LOG.infof("Eddi '%s' status: phase=%s, readyReplicas=%d/%d",
                eddi.getMetadata().getName(),
                status.getPhase(),
                status.getReadyReplicas(),
                status.getReplicas());

        return UpdateControl.patchStatus(eddi);
    }

    @Override
    public ErrorStatusUpdateControl<EddiResource> updateErrorStatus(
            EddiResource eddi,
            Context<EddiResource> context,
            Exception e) {
        LOG.errorf(e, "Error reconciling Eddi '%s'", eddi.getMetadata().getName());

        var status = eddi.getStatus();
        if (status == null) {
            status = new EddiStatus();
        }
        status.setPhase("Failed");

        eddi.setStatus(status);
        return ErrorStatusUpdateControl.patchStatus(eddi);
    }
}
