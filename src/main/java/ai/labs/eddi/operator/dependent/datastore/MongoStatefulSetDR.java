package ai.labs.eddi.operator.dependent.datastore;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.List;
import java.util.Map;

/**
 * Manages a single-node MongoDB StatefulSet for development/demo use.
 * Activated only when spec.datastore.type=mongodb and spec.datastore.managed.enabled=true.
 */
@KubernetesDependent
public class MongoStatefulSetDR extends CRUDKubernetesDependentResource<StatefulSet, EddiResource> {

    public MongoStatefulSetDR() {
        super(StatefulSet.class);
    }

    @Override
    protected StatefulSet desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var managed = spec.getDatastore().getManaged();
        var name = Labels.resourceName(eddi, "mongodb");

        var resources = new ResourceRequirementsBuilder()
                .withRequests(Map.of(
                        "cpu", new Quantity(managed.getResources().getRequests().getCpu()),
                        "memory", new Quantity(managed.getResources().getRequests().getMemory())
                ))
                .withLimits(Map.of(
                        "cpu", new Quantity(managed.getResources().getLimits().getCpu()),
                        "memory", new Quantity(managed.getResources().getLimits().getMemory())
                ))
                .build();

        var storageSize = managed.getStorage().getSize();
        var storageClassName = managed.getStorage().getStorageClassName();

        var pvcSpecBuilder = new PersistentVolumeClaimSpecBuilder()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                    .withRequests(Map.of("storage", new Quantity(storageSize)))
                .endResources();

        if (storageClassName != null && !storageClassName.isBlank()) {
            pvcSpecBuilder.withStorageClassName(storageClassName);
        }

        var pvc = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                    .withName("data")
                .endMetadata()
                .withSpec(pvcSpecBuilder.build())
                .build();

        return new StatefulSetBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "mongodb"))
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withServiceName(name)
                    .withNewSelector()
                        .withMatchLabels(Labels.selector(eddi, "mongodb"))
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(Labels.standard(eddi, "mongodb"))
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("mongodb")
                                .withImage("mongo:7.0")
                                .withPorts(List.of(
                                        new ContainerPortBuilder()
                                                .withName("mongodb")
                                                .withContainerPort(27017)
                                                .build()
                                ))
                                .withResources(resources)
                                .addNewVolumeMount()
                                    .withName("data")
                                    .withMountPath("/data/db")
                                .endVolumeMount()
                                .withNewReadinessProbe()
                                    .withNewExec()
                                        .withCommand("mongosh", "--eval", "db.adminCommand('ping')")
                                    .endExec()
                                    .withInitialDelaySeconds(10)
                                    .withPeriodSeconds(10)
                                .endReadinessProbe()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                    .withVolumeClaimTemplates(pvc)
                .endSpec()
                .build();
    }
}
