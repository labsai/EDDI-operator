package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.BackupStorageType;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * Activates the BackupPvcDR when spec.backup.enabled=true and spec.backup.storage.type=pvc.
 */
public class BackupPvcActivationCondition implements Condition<HasMetadata, EddiResource> {

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        var backup = eddi.getSpec().getBackup();
        return backup.isEnabled() && BackupStorageType.PVC == backup.getStorage().getType();
    }
}
