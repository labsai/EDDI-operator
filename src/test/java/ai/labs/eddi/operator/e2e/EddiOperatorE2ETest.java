package ai.labs.eddi.operator.e2e;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * E2E test scaffold using Testcontainers K3s.
 * <p>
 * This test validates the operator's behavior in a real Kubernetes cluster:
 * <ol>
 *   <li>Starts a K3s container (lightweight single-node Kubernetes)</li>
 *   <li>Installs the EDDI CRD</li>
 *   <li>Applies a minimal Eddi CR</li>
 *   <li>Verifies the operator creates all expected child resources</li>
 * </ol>
 * <p>
 * <strong>Prerequisites:</strong>
 * <ul>
 *   <li>Docker must be running</li>
 *   <li>The operator container image must be pre-built and available</li>
 *   <li>The CRD YAML must be at {@code target/kubernetes/eddis.eddi.labs.ai-v1.yml}</li>
 * </ul>
 * <p>
 * <strong>To enable:</strong> Remove the @Disabled annotation and ensure the
 * operator image is built via {@code mvn package -Dquarkus.container-image.build=true}.
 */
@EnabledOnOs(OS.LINUX)
@Disabled("Requires pre-built operator image. Enable after CI image pipeline is ready.")
class EddiOperatorE2ETest {

    private static final String K3S_IMAGE = "rancher/k3s:v1.31.4-k3s1";
    private static final String NAMESPACE = "e2e-test";

    private static K3sContainer k3s;
    private static KubernetesClient client;

    @BeforeAll
    static void startCluster() throws Exception {
        k3s = new K3sContainer(DockerImageName.parse(K3S_IMAGE));
        k3s.start();

        var kubeconfig = k3s.getKubeConfigYaml();
        var config = Config.fromKubeconfig(kubeconfig);
        client = new KubernetesClientBuilder().withConfig(config).build();

        // Create test namespace
        client.namespaces().resource(new NamespaceBuilder()
                        .withNewMetadata().withName(NAMESPACE).endMetadata()
                        .build())
                .create();

        // Install CRD
        installCrd();
    }

    @AfterAll
    static void stopCluster() {
        if (client != null) {
            try {
                client.namespaces().withName(NAMESPACE).delete();
            } catch (Exception ignored) {
                // Best-effort cleanup
            }
            client.close();
        }
        if (k3s != null) {
            k3s.stop();
        }
    }

    @Test
    void clusterShouldBeReachable() {
        var version = client.getKubernetesVersion();
        assertThat(version).isNotNull();
        assertThat(version.getMajor()).isEqualTo("1");
        System.out.println("K3s cluster version: " + version.getGitVersion());
    }

    @Test
    void crdShouldBeInstalled() {
        var crd = client.apiextensions().v1().customResourceDefinitions()
                .withName("eddis.eddi.labs.ai")
                .get();
        assertThat(crd).isNotNull();
        assertThat(crd.getSpec().getVersions()).isNotEmpty();
    }

    // ────────────────────────────────────────────────────
    //  Future tests (enable after operator image is available):
    //
    //  @Test void shouldCreateDeploymentFromCR() { ... }
    //  @Test void shouldCreateServiceFromCR() { ... }
    //  @Test void shouldTransitionToRunningPhase() { ... }
    //  @Test void shouldCleanupOnCRDeletion() { ... }
    // ────────────────────────────────────────────────────

    private static void installCrd() throws IOException {
        // Try the generated CRD first, then fallback to classpath
        InputStream crdStream = EddiOperatorE2ETest.class.getClassLoader()
                .getResourceAsStream("META-INF/fabric8/eddis.eddi.labs.ai-v1.yml");

        if (crdStream == null) {
            System.out.println("WARN: CRD YAML not found on classpath. " +
                    "Run 'mvn generate-resources' first to generate it.");
            return;
        }

        try (crdStream) {
            var crd = client.apiextensions().v1().customResourceDefinitions()
                    .load(crdStream).item();
            client.apiextensions().v1().customResourceDefinitions()
                    .resource(crd).create();
            System.out.println("CRD installed: " + crd.getMetadata().getName());
        }
    }
}
