package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * ReadyPostcondition for the NATS StatefulSet.
 * Returns true when managed NATS has at least one ready replica,
 * or when using in-memory messaging or external NATS.
 * Has no-arg constructor for JOSDK reflection compatibility.
 */
public class NatsReadyCondition implements Condition<HasMetadata, EddiResource> {

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        if ("in-memory".equals(eddi.getSpec().getMessaging().getType())) {
            return true;
        }
        if (!eddi.getSpec().getMessaging().getManaged().isEnabled()) {
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
            return false;
        }
    }
}
