package ai.labs.eddi.operator.conditions;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.DatastoreType;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import org.jboss.logging.Logger;

/**
 * ReadyPostcondition for the PostgreSQL StatefulSet.
 * Returns true when the managed PostgreSQL has at least one ready replica,
 * or when PostgreSQL is external (assumed ready).
 */
public class PostgresReadyCondition implements Condition<HasMetadata, EddiResource> {

    private static final Logger LOG = Logger.getLogger(PostgresReadyCondition.class);

    @Override
    public boolean isMet(DependentResource<HasMetadata, EddiResource> dependentResource,
                          EddiResource eddi,
                          Context<EddiResource> context) {
        if (!eddi.getSpec().getDatastore().getManaged().isEnabled()) {
            return true; // External DB assumed ready
        }
        if (DatastoreType.POSTGRES != eddi.getSpec().getDatastore().getType()) {
            return true; // Not using PostgreSQL
        }

        var stsName = Labels.resourceName(eddi, "postgres");
        try {
            var sts = context.getClient().apps().statefulSets()
                    .inNamespace(eddi.getMetadata().getNamespace())
                    .withName(stsName)
                    .get();
            return sts != null && sts.getStatus() != null
                    && sts.getStatus().getReadyReplicas() != null
                    && sts.getStatus().getReadyReplicas() > 0;
        } catch (Exception e) {
            LOG.debugf(e, "PostgreSQL StatefulSet '%s' not available yet", stsName);
            return false;
        }
    }
}
