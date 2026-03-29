package ai.labs.eddi.operator.dependent.extras;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudget;
import io.fabric8.kubernetes.api.model.policy.v1.PodDisruptionBudgetBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

/**
 * Manages the PodDisruptionBudget for the EDDI Deployment.
 * Activated when spec.podDisruptionBudget.enabled=true.
 */
@KubernetesDependent
public class PdbDR extends CRUDKubernetesDependentResource<PodDisruptionBudget, EddiResource> {

    public PdbDR() {
        super(PodDisruptionBudget.class);
    }

    @Override
    protected PodDisruptionBudget desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var pdb = spec.getPodDisruptionBudget();
        var name = Labels.resourceName(eddi, "pdb");

        return new PodDisruptionBudgetBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "pdb"))
                .endMetadata()
                .withNewSpec()
                    .withMinAvailable(new IntOrString(pdb.getMinAvailable()))
                    .withNewSelector()
                        .withMatchLabels(Labels.selector(eddi, "server"))
                    .endSelector()
                .endSpec()
                .build();
    }
}
