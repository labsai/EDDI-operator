package ai.labs.eddi.operator.dependent.extras;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.List;
import java.util.Map;

/**
 * Manages the EDDI Manager (config UI) Deployment.
 * Activated when spec.manager.enabled=true.
 */
@KubernetesDependent
public class ManagerDeploymentDR extends CRUDKubernetesDependentResource<Deployment, EddiResource> {

    public ManagerDeploymentDR() {
        super(Deployment.class);
    }

    @Override
    protected Deployment desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var managerSpec = spec.getManager();
        var name = Labels.resourceName(eddi, "manager");

        var imageRepo = managerSpec.getImage().getRepository();
        var imageTag = managerSpec.getImage().getTag();
        var image = imageRepo + ":" + ((imageTag == null || imageTag.isBlank()) ? "latest" : imageTag);

        var resources = new ResourceRequirementsBuilder()
                .withRequests(Map.of(
                        "cpu", new Quantity(managerSpec.getResources().getRequests().getCpu()),
                        "memory", new Quantity(managerSpec.getResources().getRequests().getMemory())
                ))
                .withLimits(Map.of(
                        "cpu", new Quantity(managerSpec.getResources().getLimits().getCpu()),
                        "memory", new Quantity(managerSpec.getResources().getLimits().getMemory())
                ))
                .build();

        var eddiServiceName = Labels.resourceName(eddi, "server");

        return new DeploymentBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "manager"))
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withNewSelector()
                        .withMatchLabels(Labels.selector(eddi, "manager"))
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(Labels.standard(eddi, "manager"))
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("manager")
                                .withImage(image)
                                .withImagePullPolicy(managerSpec.getImage().getPullPolicy())
                                .withPorts(List.of(
                                        new ContainerPortBuilder()
                                                .withName("http")
                                                .withContainerPort(3000)
                                                .build()
                                ))
                                .withEnv(List.of(
                                        new EnvVarBuilder()
                                                .withName("EDDI_API_URL")
                                                .withValue("http://" + eddiServiceName + ":8080")
                                                .build()
                                ))
                                .withResources(resources)
                                .withNewReadinessProbe()
                                    .withNewHttpGet()
                                        .withPath("/")
                                        .withPort(new IntOrString(3000))
                                    .endHttpGet()
                                    .withInitialDelaySeconds(5)
                                    .withPeriodSeconds(10)
                                .endReadinessProbe()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }
}
