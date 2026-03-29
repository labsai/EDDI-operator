package ai.labs.eddi.operator.dependent.exposure;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.networking.v1.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a Kubernetes Ingress for exposing the EDDI server.
 * Activated when exposure.type=ingress or when auto-detected on vanilla K8s.
 */
@KubernetesDependent
public class IngressDR extends CRUDKubernetesDependentResource<Ingress, EddiResource> {

    public IngressDR() {
        super(Ingress.class);
    }

    @Override
    protected Ingress desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var exposure = spec.getExposure();
        var name = Labels.resourceName(eddi, "ingress");
        var serviceName = Labels.resourceName(eddi, "server");
        var host = exposure.getHost();

        var ingressSpec = new IngressSpecBuilder();

        // Ingress class
        var ingressClassName = exposure.getIngressClassName();
        if (ingressClassName != null && !ingressClassName.isBlank()) {
            ingressSpec.withIngressClassName(ingressClassName);
        }

        // TLS
        if (exposure.getTls().isEnabled()) {
            var tlsBuilder = new IngressTLSBuilder().withHosts(host);
            var tlsSecret = exposure.getTls().getSecretRef();
            if (tlsSecret != null && !tlsSecret.isBlank()) {
                tlsBuilder.withSecretName(tlsSecret);
            }
            ingressSpec.withTls(List.of(tlsBuilder.build()));
        }

        // Rules
        ingressSpec.withRules(List.of(
                new IngressRuleBuilder()
                        .withHost(host)
                        .withNewHttp()
                            .withPaths(List.of(
                                    new HTTPIngressPathBuilder()
                                            .withPath("/")
                                            .withPathType("Prefix")
                                            .withNewBackend()
                                                .withNewService()
                                                    .withName(serviceName)
                                                    .withNewPort()
                                                        .withNumber(8080)
                                                    .endPort()
                                                .endService()
                                            .endBackend()
                                            .build()
                            ))
                        .endHttp()
                        .build()
        ));

        return new IngressBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "ingress"))
                    .withAnnotations(exposure.getAnnotations())
                .endMetadata()
                .withSpec(ingressSpec.build())
                .build();
    }
}
