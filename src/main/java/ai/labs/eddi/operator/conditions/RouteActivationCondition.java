package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.ExposureType;
import ai.labs.eddi.operator.util.OpenShiftDetector;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * Activates the Route DR when running on OpenShift and exposure type is AUTO or ROUTE.
 * Delegates OpenShift detection to the shared OpenShiftDetector utility.
 */
public class RouteActivationCondition implements Condition<HasMetadata, EddiResource> {

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        var type = eddi.getSpec().getExposure().getType();
        if (type == ExposureType.NONE || type == ExposureType.INGRESS) {
            return false;
        }

        return OpenShiftDetector.isOpenShift(context);
    }
}
