package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * Activates NetworkPolicyDR only when spec.networkPolicy=true.
 */
public class NetworkPolicyActivationCondition implements Condition<HasMetadata, EddiResource> {

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        return eddi.getSpec().isNetworkPolicy();
    }
}
