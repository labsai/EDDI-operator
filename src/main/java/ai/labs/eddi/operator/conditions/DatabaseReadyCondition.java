package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * Aggregate ready condition that checks whichever managed database is active.
 * Delegates to the appropriate datastore type check.
 * Has no-arg constructor for JOSDK reflection compatibility.
 *
 * @deprecated Use MongoReadyCondition or PostgresReadyCondition instead.
 */
@Deprecated
public class DatabaseReadyCondition implements Condition<HasMetadata, EddiResource> {

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        if (!eddi.getSpec().getDatastore().getManaged().isEnabled()) {
            return true;
        }

        var type = eddi.getSpec().getDatastore().getType();
        var component = "mongodb".equals(type) ? "mongodb" : "postgres";
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
            return false;
        }
    }
}
