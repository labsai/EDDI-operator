package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * Activates VaultSecretDR only when spec.vault.masterKeySecretRef is NOT set.
 * When the user provides their own secret ref, the operator must not create or overwrite it.
 */
public class VaultSecretActivationCondition implements Condition<Secret, EddiResource> {

    @Override
    public boolean isMet(DependentResource<Secret, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        var ref = eddi.getSpec().getVault().getMasterKeySecretRef();
        return ref == null || ref.isBlank();
    }
}
