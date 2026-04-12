package ai.labs.eddi.operator.integration;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.crd.spec.DatastoreType;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the EDDI Operator reconciliation loop.
 * Uses QuarkusTest + WithKubernetesTestServer to verify that creating an Eddi CR
 * triggers the creation of all expected child resources.
 *
 * The @WithKubernetesTestServer annotation starts a mock Kubernetes API server
 * in CRUD mode (the default for Quarkus) and configures the injected
 * KubernetesClient to use it automatically.
 */
@QuarkusTest
@WithKubernetesTestServer
class EddiReconcilerIT {

    @Inject
    KubernetesClient client;

    private static final String TEST_NS = "default";
    private static final String CR_NAME = "test-eddi";

    /**
     * Creates a minimal Eddi CR for testing.
     */
    private EddiResource createMinimalEddi() {
        var eddi = new EddiResource();
        var meta = new ObjectMeta();
        meta.setName(CR_NAME);
        meta.setNamespace(TEST_NS);
        eddi.setMetadata(meta);

        var spec = new EddiSpec();
        spec.setVersion("6.0.0");
        spec.setReplicas(1);
        spec.getDatastore().setType(DatastoreType.MONGODB);
        spec.getDatastore().getManaged().setEnabled(true);
        eddi.setSpec(spec);

        return eddi;
    }

    /**
     * Creates an Eddi CR with PostgreSQL configuration.
     */
    private EddiResource createPostgresEddi() {
        var eddi = createMinimalEddi();
        eddi.getSpec().getDatastore().setType(DatastoreType.POSTGRES);
        eddi.getSpec().getDatastore().getManaged().setEnabled(true);
        return eddi;
    }

    @Test
    void shouldCreateConfigMapWhenEddiCRIsCreated() {
        var eddi = createMinimalEddi();
        client.resource(eddi).inNamespace(TEST_NS).createOrReplace();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var configMap = client.configMaps()
                    .inNamespace(TEST_NS)
                    .withName(CR_NAME + "-config")
                    .get();
            assertThat(configMap).isNotNull();
            assertThat(configMap.getData())
                    .containsEntry("EDDI_DATASTORE_TYPE", "mongodb");
        });
    }

    @Test
    void shouldCreateServiceAccountWhenEddiCRIsCreated() {
        var eddi = createMinimalEddi();
        client.resource(eddi).inNamespace(TEST_NS).createOrReplace();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var sa = client.serviceAccounts()
                    .inNamespace(TEST_NS)
                    .withName(CR_NAME + "-server")
                    .get();
            assertThat(sa).isNotNull();
            assertThat(sa.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/managed-by", "eddi-operator");
        });
    }

    @Test
    void shouldCreateEddiDeployment() {
        var eddi = createMinimalEddi();
        client.resource(eddi).inNamespace(TEST_NS).createOrReplace();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var deployment = client.apps().deployments()
                    .inNamespace(TEST_NS)
                    .withName(CR_NAME + "-server")
                    .get();
            assertThat(deployment).isNotNull();
            assertThat(deployment.getSpec().getReplicas()).isEqualTo(1);
            assertThat(deployment.getSpec().getTemplate().getSpec().getContainers())
                    .anyMatch(c -> c.getImage().equals("labsai/eddi:6.0.0"));
        });
    }

    @Test
    void shouldCreateEddiService() {
        var eddi = createMinimalEddi();
        client.resource(eddi).inNamespace(TEST_NS).createOrReplace();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var service = client.services()
                    .inNamespace(TEST_NS)
                    .withName(CR_NAME + "-server")
                    .get();
            assertThat(service).isNotNull();
        });
    }

    @Test
    void shouldCreateMongoStatefulSetForManagedMongo() {
        var eddi = createMinimalEddi();
        client.resource(eddi).inNamespace(TEST_NS).createOrReplace();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var sts = client.apps().statefulSets()
                    .inNamespace(TEST_NS)
                    .withName(CR_NAME + "-mongodb")
                    .get();
            assertThat(sts).isNotNull();
        });
    }

    @Test
    void shouldCreatePostgresResourcesForManagedPostgres() {
        var eddi = createPostgresEddi();
        client.resource(eddi).inNamespace(TEST_NS).createOrReplace();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var sts = client.apps().statefulSets()
                    .inNamespace(TEST_NS)
                    .withName(CR_NAME + "-postgres")
                    .get();
            assertThat(sts).isNotNull();

            var secret = client.secrets()
                    .inNamespace(TEST_NS)
                    .withName(CR_NAME + "-postgres-credentials")
                    .get();
            assertThat(secret).isNotNull();
        });
    }

    @Test
    void shouldCreateVaultSecretWhenNoExternalRef() {
        var eddi = createMinimalEddi();
        eddi.getSpec().getVault().setMasterKeySecretRef("");
        client.resource(eddi).inNamespace(TEST_NS).createOrReplace();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var secret = client.secrets()
                    .inNamespace(TEST_NS)
                    .withName(CR_NAME + "-vault-key")
                    .get();
            assertThat(secret).isNotNull();
            assertThat(secret.getData()).containsKey("master-key");
        });
    }

    @Test
    void shouldConfigurePostgresProfileInConfigMap() {
        var eddi = createPostgresEddi();
        client.resource(eddi).inNamespace(TEST_NS).createOrReplace();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var configMap = client.configMaps()
                    .inNamespace(TEST_NS)
                    .withName(CR_NAME + "-config")
                    .get();
            assertThat(configMap).isNotNull();
            assertThat(configMap.getData())
                    .containsEntry("EDDI_DATASTORE_TYPE", "postgres")
                    .containsEntry("QUARKUS_PROFILE", "postgres");
        });
    }

    @Test
    void shouldSetCorrectLabelsOnAllResources() {
        var eddi = createMinimalEddi();
        client.resource(eddi).inNamespace(TEST_NS).createOrReplace();

        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            var configMap = client.configMaps()
                    .inNamespace(TEST_NS)
                    .withName(CR_NAME + "-config")
                    .get();
            assertThat(configMap).isNotNull();
            assertThat(configMap.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/name", "eddi")
                    .containsEntry("app.kubernetes.io/instance", CR_NAME)
                    .containsEntry("app.kubernetes.io/managed-by", "eddi-operator");
        });
    }
}
