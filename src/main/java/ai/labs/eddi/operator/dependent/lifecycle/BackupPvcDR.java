package ai.labs.eddi.operator.dependent.lifecycle;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.Map;

/**
 * Manages a PVC for backup storage when spec.backup.storage.type=pvc.
 * Activated by BackupPvcActivationCondition.
 */
@KubernetesDependent
public class BackupPvcDR extends CRUDKubernetesDependentResource<PersistentVolumeClaim, EddiResource> {

    public BackupPvcDR() {
        super(PersistentVolumeClaim.class);
    }

    @Override
    protected PersistentVolumeClaim desired(EddiResource eddi, Context<EddiResource> context) {
        var backup = eddi.getSpec().getBackup();
        var pvcSpec = backup.getStorage().getPvc();
        var name = Labels.resourceName(eddi, "backup-pvc");

        var specBuilder = new PersistentVolumeClaimSpecBuilder()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                    .withRequests(Map.of("storage", new Quantity(pvcSpec.getSize())))
                .endResources();

        if (pvcSpec.getStorageClassName() != null && !pvcSpec.getStorageClassName().isBlank()) {
            specBuilder.withStorageClassName(pvcSpec.getStorageClassName());
        }

        return new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "backup"))
                .endMetadata()
                .withSpec(specBuilder.build())
                .build();
    }
}
