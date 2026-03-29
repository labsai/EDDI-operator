package ai.labs.eddi.operator.dependent.extras;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.IntOrString;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

/**
 * Manages a NetworkPolicy restricting ingress to the EDDI server.
 * Only allows traffic from within the namespace and from Ingress/Route controllers.
 */
@KubernetesDependent
public class NetworkPolicyDR extends CRUDKubernetesDependentResource<NetworkPolicy, EddiResource> {

    public NetworkPolicyDR() {
        super(NetworkPolicy.class);
    }

    @Override
    protected NetworkPolicy desired(EddiResource eddi, Context<EddiResource> context) {
        var name = Labels.resourceName(eddi, "network-policy");

        return new NetworkPolicyBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "network-policy"))
                .endMetadata()
                .withNewSpec()
                    .withNewPodSelector()
                        .withMatchLabels(Labels.selector(eddi, "server"))
                    .endPodSelector()
                    .withPolicyTypes("Ingress")
                    .addNewIngress()
                        // Allow from same namespace
                        .addNewFrom()
                            .withNewPodSelector()
                            .endPodSelector()
                        .endFrom()
                        .addNewPort()
                            .withPort(new IntOrString(8080))
                            .withProtocol("TCP")
                        .endPort()
                    .endIngress()
                .endSpec()
                .build();
    }
}
