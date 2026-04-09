package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import org.jboss.logging.Logger;

/**
 * Activates the Route DR when running on OpenShift and exposure type is "auto" or "route".
 * Caches the OpenShift detection result for the lifecycle of the condition instance.
 */
public class RouteActivationCondition implements Condition<HasMetadata, EddiResource> {

    private static final Logger LOG = Logger.getLogger(RouteActivationCondition.class);

    private volatile Boolean isOpenShift = null;

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        var type = eddi.getSpec().getExposure().getType();
        if ("none".equals(type) || "ingress".equals(type)) {
            return false;
        }

        return isOpenShift(context);
    }

    private boolean isOpenShift(Context<EddiResource> context) {
        if (isOpenShift != null) {
            return isOpenShift;
        }
        try {
            var routeCrd = context.getClient().apiextensions().v1()
                    .customResourceDefinitions()
                    .withName("routes.route.openshift.io")
                    .get();
            isOpenShift = routeCrd != null;
        } catch (Exception e) {
            LOG.debugf(e, "OpenShift Route CRD not detected, assuming vanilla Kubernetes");
            isOpenShift = false;
        }
        return isOpenShift;
    }
}
