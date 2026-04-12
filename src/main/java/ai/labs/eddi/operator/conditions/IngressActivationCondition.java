package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.ExposureType;
import ai.labs.eddi.operator.util.OpenShiftDetector;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * Activates the Ingress DR when NOT on OpenShift and exposure type is AUTO or INGRESS.
 * Delegates OpenShift detection to the shared OpenShiftDetector utility.
 */
public class IngressActivationCondition implements Condition<HasMetadata, EddiResource> {

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        var type = eddi.getSpec().getExposure().getType();
        if (type == ExposureType.NONE || type == ExposureType.ROUTE) {
            return false;
        }

        if (type == ExposureType.INGRESS) {
            return true;
        }

        // auto mode — use Ingress if Route CRD is NOT available
        return !OpenShiftDetector.isOpenShift(context);
    }
}
