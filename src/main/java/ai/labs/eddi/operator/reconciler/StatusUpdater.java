package ai.labs.eddi.operator.reconciler;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiStatus;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Computes the EddiStatus from the state of child resources.
 * Follows Kubernetes conventions:
 * - lastTransitionTime only updates when condition status actually changes
 * - observedGeneration tracks which generation was last reconciled
 */
@ApplicationScoped
public class StatusUpdater {

    private static final Logger LOG = Logger.getLogger(StatusUpdater.class);

    /**
     * Computes the full status for the Eddi CR based on child resource states.
     */
    public EddiStatus computeStatus(EddiResource eddi, Context<EddiResource> context) {
        var status = eddi.getStatus() != null ? eddi.getStatus() : new EddiStatus();
        var spec = eddi.getSpec();
        var namespace = eddi.getMetadata().getNamespace();

        // Fix #8: Set observedGeneration
        status.setObservedGeneration(eddi.getMetadata().getGeneration());
        status.setVersion(spec.getVersion());
        status.setReplicas(spec.getReplicas());

        // Index existing conditions by type for transition detection
        Map<String, Condition> existingConditions = status.getConditions() != null
                ? status.getConditions().stream()
                    .collect(Collectors.toMap(Condition::getType, c -> c, (a, b) -> b))
                : Map.of();

        // Check EDDI Deployment readiness
        var deploymentName = Labels.resourceName(eddi, "server");
        int readyReplicas = 0;
        boolean isDeploying = false;

        try {
            var deployment = context.getClient().apps().deployments()
                    .inNamespace(namespace)
                    .withName(deploymentName)
                    .get();

            if (deployment != null && deployment.getStatus() != null) {
                var depStatus = deployment.getStatus();
                readyReplicas = depStatus.getReadyReplicas() != null
                        ? depStatus.getReadyReplicas() : 0;
                isDeploying = depStatus.getUpdatedReplicas() != null
                        && depStatus.getUpdatedReplicas() < spec.getReplicas();
            }
        } catch (Exception e) {
            LOG.debugf(e, "EDDI Deployment not found yet for '%s'", eddi.getMetadata().getName());
        }

        status.setReadyReplicas(readyReplicas);

        // Compute conditions
        var conditions = new ArrayList<Condition>();

        // Available condition
        boolean available = readyReplicas > 0;
        conditions.add(buildCondition("Available",
                available, "DeploymentAvailable", "NoReadyReplicas",
                available ? readyReplicas + " replica(s) ready" : "Waiting for replicas to become ready",
                available ? null : "Waiting for replicas to become ready",
                existingConditions));

        // DatastoreReady condition
        boolean dbReady = checkDatastoreReady(eddi, context);
        conditions.add(buildCondition("DatastoreReady",
                dbReady, "DatastoreConnected", "DatastoreNotReady",
                dbReady ? "Datastore is ready" : "Waiting for datastore",
                dbReady ? null : "Waiting for datastore",
                existingConditions));

        // MessagingReady condition
        boolean msgReady = checkMessagingReady(eddi, context);
        conditions.add(buildCondition("MessagingReady",
                msgReady, "MessagingConnected", "MessagingNotReady",
                msgReady ? "Messaging is ready" : "Waiting for messaging",
                msgReady ? null : "Waiting for messaging",
                existingConditions));

        // Progressing condition
        conditions.add(buildCondition("Progressing",
                isDeploying, "RollingUpdate", "DeploymentComplete",
                isDeploying ? "Deployment is rolling out" : "All replicas updated",
                isDeploying ? null : "All replicas updated",
                existingConditions));

        // Degraded condition
        boolean degraded = available && readyReplicas < spec.getReplicas();
        conditions.add(buildCondition("Degraded",
                degraded, "PartiallyAvailable", "FullyAvailable",
                degraded ? readyReplicas + "/" + spec.getReplicas() + " replicas ready" : "All replicas ready",
                degraded ? null : "All replicas ready",
                existingConditions));

        status.setConditions(conditions);

        // Compute phase
        if (!dbReady || !msgReady) {
            status.setPhase("Pending");
        } else if (isDeploying) {
            status.setPhase("Deploying");
        } else if (available && readyReplicas >= spec.getReplicas()) {
            status.setPhase("Running");
        } else if (available) {
            status.setPhase("Deploying");
        } else {
            status.setPhase("Pending");
        }

        // Resolve URL
        status.setUrl(resolveUrl(eddi, context));

        return status;
    }

    /**
     * Builds a Condition, preserving lastTransitionTime if the status hasn't changed.
     */
    private Condition buildCondition(String type, boolean isTrue,
                                      String trueReason, String falseReason,
                                      String trueMessage, String falseMessage,
                                      Map<String, Condition> existingConditions) {
        var statusStr = isTrue ? "True" : "False";
        var reason = isTrue ? trueReason : falseReason;
        var message = isTrue ? trueMessage : falseMessage;

        // Fix #5: Only update lastTransitionTime when the status actually changes
        var now = Instant.now().toString();
        var transitionTime = now;
        var existing = existingConditions.get(type);
        if (existing != null && statusStr.equals(existing.getStatus())) {
            // Status hasn't changed — preserve the original transition time
            transitionTime = existing.getLastTransitionTime() != null
                    ? existing.getLastTransitionTime() : now;
        }

        return new ConditionBuilder()
                .withType(type)
                .withStatus(statusStr)
                .withReason(reason)
                .withMessage(message)
                .withLastTransitionTime(transitionTime)
                .build();
    }

    private boolean checkDatastoreReady(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        if (!spec.getDatastore().getManaged().isEnabled()) {
            return true; // External DB assumed ready
        }

        var component = "mongodb".equals(spec.getDatastore().getType()) ? "mongodb" : "postgres";
        var stsName = Labels.resourceName(eddi, component);

        try {
            var sts = context.getClient().apps().statefulSets()
                    .inNamespace(eddi.getMetadata().getNamespace())
                    .withName(stsName)
                    .get();
            return sts != null && sts.getStatus() != null
                    && sts.getStatus().getReadyReplicas() != null
                    && sts.getStatus().getReadyReplicas() > 0;
        } catch (Exception e) {
            LOG.debugf(e, "Datastore StatefulSet '%s' not available yet", stsName);
            return false;
        }
    }

    private boolean checkMessagingReady(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        if ("in-memory".equals(spec.getMessaging().getType())) {
            return true;
        }
        if (!spec.getMessaging().getManaged().isEnabled()) {
            return true; // External NATS assumed ready
        }

        var stsName = Labels.resourceName(eddi, "nats");
        try {
            var sts = context.getClient().apps().statefulSets()
                    .inNamespace(eddi.getMetadata().getNamespace())
                    .withName(stsName)
                    .get();
            return sts != null && sts.getStatus() != null
                    && sts.getStatus().getReadyReplicas() != null
                    && sts.getStatus().getReadyReplicas() > 0;
        } catch (Exception e) {
            LOG.debugf(e, "NATS StatefulSet '%s' not available yet", stsName);
            return false;
        }
    }

    private String resolveUrl(EddiResource eddi, Context<EddiResource> context) {
        var namespace = eddi.getMetadata().getNamespace();

        // Try Route first (OpenShift)
        try {
            var routeName = Labels.resourceName(eddi, "route");
            var route = context.getClient().adapt(io.fabric8.openshift.client.OpenShiftClient.class)
                    .routes()
                    .inNamespace(namespace)
                    .withName(routeName)
                    .get();
            if (route != null && route.getSpec() != null && route.getSpec().getHost() != null) {
                var scheme = route.getSpec().getTls() != null ? "https" : "http";
                return scheme + "://" + route.getSpec().getHost();
            }
        } catch (Exception e) {
            LOG.debugf(e, "Route not found for '%s' (expected on vanilla K8s)", eddi.getMetadata().getName());
        }

        // Try Ingress
        try {
            var ingressName = Labels.resourceName(eddi, "ingress");
            var ingress = context.getClient().network().v1().ingresses()
                    .inNamespace(namespace)
                    .withName(ingressName)
                    .get();
            if (ingress != null && ingress.getSpec() != null
                    && ingress.getSpec().getRules() != null
                    && !ingress.getSpec().getRules().isEmpty()) {
                var host = ingress.getSpec().getRules().get(0).getHost();
                var scheme = (ingress.getSpec().getTls() != null
                        && !ingress.getSpec().getTls().isEmpty()) ? "https" : "http";
                return scheme + "://" + host;
            }
        } catch (Exception e) {
            LOG.debugf(e, "Ingress not found for '%s'", eddi.getMetadata().getName());
        }

        return "";
    }
}
