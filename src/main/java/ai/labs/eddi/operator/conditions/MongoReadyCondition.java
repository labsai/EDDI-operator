package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * ReadyPostcondition for the MongoDB StatefulSet.
 * Returns true when the managed MongoDB has at least one ready replica,
 * or when MongoDB is external (assumed ready).
 */
public class MongoReadyCondition implements Condition<HasMetadata, EddiResource> {

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        if (!eddi.getSpec().getDatastore().getManaged().isEnabled()) {
            return true; // External DB assumed ready
        }
        if (!"mongodb".equals(eddi.getSpec().getDatastore().getType())) {
            return true; // Not using MongoDB
        }

        var stsName = Labels.resourceName(eddi, "mongodb");
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
