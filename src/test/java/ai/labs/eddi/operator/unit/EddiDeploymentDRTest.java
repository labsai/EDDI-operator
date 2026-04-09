package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.dependent.core.EddiDeploymentDR;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for EddiDeploymentDR — verifies the desired() output
 * produces a correct Deployment for various EddiSpec configurations.
 *
 * Since desired() requires a JOSDK Context, we test the helper methods
 * and verify the overall structure using reflection where needed.
 */
class EddiDeploymentDRTest {

    private EddiResource eddi;

    @BeforeEach
    void setUp() {
        eddi = new EddiResource();
        var meta = new ObjectMeta();
        meta.setName("my-eddi");
        meta.setNamespace("test-ns");
        eddi.setMetadata(meta);
        eddi.setSpec(new EddiSpec());
    }

    @Test
    void shouldResolveVaultSecretNameFromSpec() throws Exception {
        // When masterKeySecretRef is set, use it
        eddi.getSpec().getVault().setMasterKeySecretRef("custom-vault-secret");

        var dr = new EddiDeploymentDR();
        var method = EddiDeploymentDR.class.getDeclaredMethod("resolveVaultSecretName", EddiResource.class);
        method.setAccessible(true);
        var name = (String) method.invoke(dr, eddi);

        assertThat(name).isEqualTo("custom-vault-secret");
    }

    @Test
    void shouldAutoGenerateVaultSecretNameWhenEmpty() throws Exception {
        // When masterKeySecretRef is empty, auto-generate
        eddi.getSpec().getVault().setMasterKeySecretRef("");

        var dr = new EddiDeploymentDR();
        var method = EddiDeploymentDR.class.getDeclaredMethod("resolveVaultSecretName", EddiResource.class);
        method.setAccessible(true);
        var name = (String) method.invoke(dr, eddi);

        assertThat(name).isEqualTo("my-eddi-vault-key");
    }

    @Test
    void shouldAutoGenerateVaultSecretNameWhenNull() throws Exception {
        // When masterKeySecretRef is null, auto-generate
        eddi.getSpec().getVault().setMasterKeySecretRef(null);

        var dr = new EddiDeploymentDR();
        var method = EddiDeploymentDR.class.getDeclaredMethod("resolveVaultSecretName", EddiResource.class);
        method.setAccessible(true);
        var name = (String) method.invoke(dr, eddi);

        assertThat(name).isEqualTo("my-eddi-vault-key");
    }

    @Test
    void shouldBuildCorrectResourceRequirements() {
        eddi.getSpec().getResources().getRequests().setCpu("500m");
        eddi.getSpec().getResources().getRequests().setMemory("512Mi");
        eddi.getSpec().getResources().getLimits().setCpu("4");
        eddi.getSpec().getResources().getLimits().setMemory("2Gi");

        var resources = ai.labs.eddi.operator.util.Defaults.buildResources(eddi.getSpec().getResources());

        assertThat(resources.getRequests().get("cpu").toString()).isEqualTo("500m");
        assertThat(resources.getRequests().get("memory").toString()).isEqualTo("512Mi");
        assertThat(resources.getLimits().get("cpu").toString()).isEqualTo("4");
        assertThat(resources.getLimits().get("memory").toString()).isEqualTo("2Gi");
    }

    @Test
    void shouldGenerateCorrectDeploymentName() {
        assertThat(Labels.resourceName(eddi, "server")).isEqualTo("my-eddi-server");
    }

    @Test
    void shouldGenerateCorrectConfigMapName() {
        assertThat(Labels.resourceName(eddi, "config")).isEqualTo("my-eddi-config");
    }

    @Test
    void shouldResolveCorrectImageWithDefaults() {
        // Default: labsai/eddi:6.0.0
        var image = ai.labs.eddi.operator.util.Defaults.resolveEddiImage(eddi.getSpec());
        assertThat(image).isEqualTo("labsai/eddi:6.0.0");
    }

    @Test
    void shouldResolveCustomImage() {
        eddi.getSpec().getImage().setRepository("myregistry/eddi");
        eddi.getSpec().getImage().setTag("custom-tag");

        var image = ai.labs.eddi.operator.util.Defaults.resolveEddiImage(eddi.getSpec());
        assertThat(image).isEqualTo("myregistry/eddi:custom-tag");
    }

    @Test
    void shouldDefaultToOneReplica() {
        assertThat(eddi.getSpec().getReplicas()).isEqualTo(1);
    }

    @Test
    void shouldDefaultPullPolicyToIfNotPresent() {
        assertThat(eddi.getSpec().getImage().getPullPolicy()).isEqualTo("IfNotPresent");
    }

    @Test
    void shouldDefaultToEmptyPullSecrets() {
        assertThat(eddi.getSpec().getImage().getPullSecrets()).isEmpty();
    }
}
