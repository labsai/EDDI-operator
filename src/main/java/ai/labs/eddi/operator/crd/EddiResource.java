package ai.labs.eddi.operator.crd;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

/**
 * Custom Resource Definition for the EDDI v6 conversational AI platform.
 * <p>
 * API Group: eddi.labs.ai
 * Version: v1beta1
 * Kind: Eddi
 */
@Group("eddi.labs.ai")
@Version("v1beta1")
@Kind("Eddi")
@Plural("eddis")
@ShortNames("eddi")
public class EddiResource extends CustomResource<EddiSpec, EddiStatus> implements Namespaced {

    @Override
    protected EddiSpec initSpec() {
        return new EddiSpec();
    }

    @Override
    protected EddiStatus initStatus() {
        return new EddiStatus();
    }
}
