package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * Activates the KeycloakSecretDR when managed auth is enabled
 * AND no external admin secret is provided (i.e., auto-generate mode).
 */
public class KeycloakSecretActivationCondition implements Condition<HasMetadata, EddiResource> {

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        var auth = eddi.getSpec().getAuth();
        if (!auth.isEnabled() || !auth.getManaged().isEnabled()) {
            return false;
        }
        // Only auto-generate if no external secret ref is provided
        var adminSecretRef = auth.getManaged().getAdminSecretRef();
        return adminSecretRef == null || adminSecretRef.isBlank();
    }
}
