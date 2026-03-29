package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Defaults;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * Activates MongoDB DRs only when spec.datastore.type=mongodb and managed.enabled=true.
 * Uses HasMetadata so this condition works for StatefulSet, Service, etc.
 */
public class MongoActivationCondition implements Condition<HasMetadata, EddiResource> {

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        return Defaults.isManagedMongodb(eddi.getSpec());
    }
}
