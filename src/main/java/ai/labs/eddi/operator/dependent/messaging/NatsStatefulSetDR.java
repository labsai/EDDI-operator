package ai.labs.eddi.operator.dependent.messaging;

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
 * Manages a NATS JetStream StatefulSet for messaging.
 * Activated only when spec.messaging.type=nats and spec.messaging.managed.enabled=true.
 */
@KubernetesDependent
public class NatsStatefulSetDR extends CRUDKubernetesDependentResource<StatefulSet, EddiResource> {

    public NatsStatefulSetDR() {
        super(StatefulSet.class);
    }

    @Override
    protected StatefulSet desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var managed = spec.getMessaging().getManaged();
        var name = Labels.resourceName(eddi, "nats");

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
                    .withLabels(Labels.standard(eddi, "nats"))
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withServiceName(name)
                    .withNewSelector()
                        .withMatchLabels(Labels.selector(eddi, "nats"))
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(Labels.standard(eddi, "nats"))
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("nats")
                                .withImage("nats:2.10-alpine")
                                .withArgs(List.of("--jetstream", "--store_dir=/data"))
                                .withPorts(List.of(
                                        new ContainerPortBuilder()
                                                .withName("client")
                                                .withContainerPort(4222)
                                                .build(),
                                        new ContainerPortBuilder()
                                                .withName("monitor")
                                                .withContainerPort(8222)
                                                .build()
                                ))
                                .withResources(resources)
                                .addNewVolumeMount()
                                    .withName("data")
                                    .withMountPath("/data")
                                .endVolumeMount()
                                .withNewReadinessProbe()
                                    .withNewHttpGet()
                                        .withPath("/healthz")
                                        .withPort(new IntOrString(8222))
                                    .endHttpGet()
                                    .withInitialDelaySeconds(5)
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
