package ai.labs.eddi.operator.dependent.exposure;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.openshift.api.model.*;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

/**
 * Manages an OpenShift Route for exposing the EDDI server.
 * Activated when Route CRD is present (i.e., running on OpenShift).
 */
@KubernetesDependent
public class RouteDR extends CRUDKubernetesDependentResource<Route, EddiResource> {

    public RouteDR() {
        super(Route.class);
    }

    @Override
    protected Route desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var exposure = spec.getExposure();
        var name = Labels.resourceName(eddi, "route");
        var serviceName = Labels.resourceName(eddi, "server");

        var routeSpecBuilder = new RouteSpecBuilder()
                .withNewTo()
                    .withKind("Service")
                    .withName(serviceName)
                    .withWeight(100)
                .endTo()
                .withNewPort()
                    .withNewTargetPort("http")
                .endPort();

        // Host (optional for Routes — OpenShift auto-generates if empty)
        var host = exposure.getHost();
        if (host != null && !host.isBlank()) {
            routeSpecBuilder.withHost(host);
        }

        // TLS termination
        if (exposure.getTls().isEnabled()) {
            routeSpecBuilder.withNewTls()
                    .withTermination("edge")
                    .withInsecureEdgeTerminationPolicy("Redirect")
                .endTls();
        }

        return new RouteBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "route"))
                    .withAnnotations(exposure.getAnnotations())
                .endMetadata()
                .withSpec(routeSpecBuilder.build())
                .build();
    }
}
