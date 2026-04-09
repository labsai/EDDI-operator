package ai.labs.eddi.operator.reconciler;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.crd.EddiStatus;

import ai.labs.eddi.operator.dependent.core.*;
import ai.labs.eddi.operator.dependent.datastore.*;
import ai.labs.eddi.operator.dependent.messaging.*;
import ai.labs.eddi.operator.dependent.auth.*;
import ai.labs.eddi.operator.dependent.exposure.*;
import ai.labs.eddi.operator.dependent.extras.*;
import ai.labs.eddi.operator.dependent.monitoring.*;

import ai.labs.eddi.operator.conditions.*;

import io.fabric8.kubernetes.api.model.EventBuilder;
import io.fabric8.kubernetes.api.model.ObjectReferenceBuilder;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Main reconciler for the Eddi custom resource.
 * Uses the @Workflow annotation to declare all dependent resources and their relationships.
 *
 * The workflow manages 26 Kubernetes resources with activation conditions,
 * readiness postconditions, and correct deployment ordering.
 */
@ControllerConfiguration(
        finalizerName = "eddi.labs.ai/cleanup",
        maxReconciliationInterval = @MaxReconciliationInterval(
                interval = 5, timeUnit = TimeUnit.MINUTES
        )
)
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
                name = "keycloak-secret",
                type = KeycloakSecretDR.class,
                activationCondition = KeycloakSecretActivationCondition.class
        ),
        @Dependent(
                name = "keycloak-deployment",
                type = KeycloakDeploymentDR.class,
                activationCondition = ManagedAuthActivationCondition.class,
                dependsOn = {"keycloak-secret"}
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
        ),

        // ── Conditional: Monitoring ──────────────────────────
        @Dependent(
                name = "service-monitor",
                type = ServiceMonitorDR.class,
                activationCondition = ServiceMonitorActivationCondition.class,
                dependsOn = {"eddi-service"}
        ),
        @Dependent(
                name = "grafana-dashboard",
                type = GrafanaDashboardDR.class,
                activationCondition = GrafanaDashboardActivationCondition.class,
                dependsOn = {"eddi-deployment"}
        ),
        @Dependent(
                name = "prometheus-rule",
                type = PrometheusRuleDR.class,
                activationCondition = AlertsActivationCondition.class,
                dependsOn = {"eddi-deployment"}
        )
})
public class EddiReconciler implements Reconciler<EddiResource>, Cleaner<EddiResource> {

    private static final Logger LOG = Logger.getLogger(EddiReconciler.class);

    private static final Set<String> VALID_DATASTORE_TYPES = Set.of("mongodb", "postgres");
    private static final Set<String> VALID_MESSAGING_TYPES = Set.of("in-memory", "nats");
    private static final Set<String> VALID_EXPOSURE_TYPES = Set.of("auto", "route", "ingress", "none");

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

        // Validate spec before proceeding
        var validationError = validateSpec(eddi.getSpec());
        if (validationError != null) {
            LOG.warnf("Eddi '%s' has invalid spec: %s",
                    eddi.getMetadata().getName(), validationError);

            emitEvent(eddi, context, "Warning", "SpecInvalid", validationError);

            var status = eddi.getStatus() != null ? eddi.getStatus() : new EddiStatus();
            status.setPhase("Failed");
            status.setObservedGeneration(eddi.getMetadata().getGeneration());
            eddi.setStatus(status);
            return UpdateControl.patchStatus(eddi);
        }

        // Status is computed from the state of child resources
        var status = statusUpdater.computeStatus(eddi, context);
        eddi.setStatus(status);

        // Emit event on phase transitions
        if ("Running".equals(status.getPhase())) {
            emitEvent(eddi, context, "Normal", "ReconcileSucceeded",
                    "EDDI is running with " + status.getReadyReplicas() + " ready replica(s)");
        } else if ("Failed".equals(status.getPhase())) {
            emitEvent(eddi, context, "Warning", "ReconcileFailed",
                    "EDDI reconciliation failed — phase: " + status.getPhase());
        }

        LOG.infof("Eddi '%s' status: phase=%s, readyReplicas=%d/%d",
                eddi.getMetadata().getName(),
                status.getPhase(),
                status.getReadyReplicas(),
                status.getReplicas());

        return UpdateControl.patchStatus(eddi);
    }

    @Override
    public DeleteControl cleanup(EddiResource eddi, Context<EddiResource> context) {
        LOG.infof("Cleaning up Eddi '%s' in namespace '%s'",
                eddi.getMetadata().getName(),
                eddi.getMetadata().getNamespace());

        emitEvent(eddi, context, "Normal", "CleanupStarted",
                "EDDI custom resource deleted, cleaning up managed resources");

        // JOSDK will garbage-collect all owner-referenced child resources.
        // This method exists for logging, event recording, and future cleanup of
        // external resources (e.g., cloud-hosted DB connections).
        return DeleteControl.defaultDelete();
    }

    @Override
    public ErrorStatusUpdateControl<EddiResource> updateErrorStatus(
            EddiResource eddi,
            Context<EddiResource> context,
            Exception e) {
        LOG.errorf(e, "Error reconciling Eddi '%s'", eddi.getMetadata().getName());

        emitEvent(eddi, context, "Warning", "ReconcileError", e.getMessage());

        var status = eddi.getStatus();
        if (status == null) {
            status = new EddiStatus();
        }
        status.setPhase("Failed");

        eddi.setStatus(status);
        return ErrorStatusUpdateControl.patchStatus(eddi);
    }

    /**
     * Validates the EddiSpec enum fields and returns an error message if invalid,
     * or null if the spec is valid.
     */
    public static String validateSpec(EddiSpec spec) {
        var dsType = spec.getDatastore().getType();
        if (!VALID_DATASTORE_TYPES.contains(dsType)) {
            return "Invalid spec.datastore.type: '" + dsType
                    + "'. Must be one of: " + VALID_DATASTORE_TYPES;
        }

        var msgType = spec.getMessaging().getType();
        if (!VALID_MESSAGING_TYPES.contains(msgType)) {
            return "Invalid spec.messaging.type: '" + msgType
                    + "'. Must be one of: " + VALID_MESSAGING_TYPES;
        }

        var expType = spec.getExposure().getType();
        if (!VALID_EXPOSURE_TYPES.contains(expType)) {
            return "Invalid spec.exposure.type: '" + expType
                    + "'. Must be one of: " + VALID_EXPOSURE_TYPES;
        }

        if (spec.getReplicas() < 1) {
            return "spec.replicas must be >= 1, got: " + spec.getReplicas();
        }

        return null; // valid
    }

    /**
     * Emits a Kubernetes Event visible via {@code kubectl describe eddi <name>}.
     * Uses v1 Event API for maximum compatibility.
     */
    private void emitEvent(EddiResource eddi, Context<EddiResource> context,
                           String type, String reason, String message) {
        try {
            var now = Instant.now().toString();
            var event = new EventBuilder()
                    .withNewMetadata()
                        .withGenerateName(eddi.getMetadata().getName() + "-")
                        .withNamespace(eddi.getMetadata().getNamespace())
                    .endMetadata()
                    .withType(type)
                    .withReason(reason)
                    .withMessage(message)
                    .withFirstTimestamp(now)
                    .withLastTimestamp(now)
                    .withNewSource()
                        .withComponent("eddi-operator")
                    .endSource()
                    .withInvolvedObject(new ObjectReferenceBuilder()
                            .withApiVersion(eddi.getApiVersion())
                            .withKind(eddi.getKind())
                            .withName(eddi.getMetadata().getName())
                            .withNamespace(eddi.getMetadata().getNamespace())
                            .withUid(eddi.getMetadata().getUid())
                            .build())
                    .build();

            context.getClient().v1().events()
                    .inNamespace(eddi.getMetadata().getNamespace())
                    .resource(event)
                    .create();
        } catch (Exception e) {
            LOG.debugf(e, "Failed to emit event '%s' for Eddi '%s'",
                    reason, eddi.getMetadata().getName());
        }
    }
}
