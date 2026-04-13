package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.DatastoreType;
import ai.labs.eddi.operator.dependent.datastore.MongoServiceDR;
import ai.labs.eddi.operator.dependent.datastore.MongoStatefulSetDR;
import ai.labs.eddi.operator.dependent.datastore.PostgresServiceDR;
import ai.labs.eddi.operator.dependent.datastore.PostgresStatefulSetDR;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static ai.labs.eddi.operator.unit.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for datastore dependent resources: MongoStatefulSetDR,
 * PostgresStatefulSetDR, MongoServiceDR, PostgresServiceDR.
 * <p>
 * PostgresSecretDR is excluded here — its {@code desired()} reads from Context
 * to preserve existing credentials. Tested via integration tests.
 * </p>
 */
class DatastoreDRTest {

    private EddiResource eddi;

    @BeforeEach
    void setUp() {
        eddi = createEddi();
    }

    // ────────────────────────────────────────────────────
    //  MongoStatefulSetDR
    // ────────────────────────────────────────────────────

    @Nested
    class MongoStatefulSet {

        @BeforeEach
        void enableManagedMongo() {
            eddi.getSpec().getDatastore().setType(DatastoreType.MONGODB);
            eddi.getSpec().getDatastore().getManaged().setEnabled(true);
        }

        @Test
        void shouldHaveCorrectNameAndNamespace() {
            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            assertThat(sts.getMetadata().getName()).isEqualTo(CR_NAME + "-mongodb");
            assertThat(sts.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
        }

        @Test
        void shouldDefaultToMongo7Image() {
            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            var image = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
            assertThat(image).isEqualTo("mongo:7.0");
        }

        @Test
        void shouldUseCustomImageWhenConfigured() {
            eddi.getSpec().getDatastore().getManaged().getImage().setRepository("custom/mongo");
            eddi.getSpec().getDatastore().getManaged().getImage().setTag("6.0");

            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            var image = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
            assertThat(image).isEqualTo("custom/mongo:6.0");
        }

        @Test
        void shouldExposePort27017() {
            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            var ports = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getPorts();
            assertThat(ports).hasSize(1);
            assertThat(ports.get(0).getContainerPort()).isEqualTo(27017);
        }

        @Test
        void shouldHaveReadinessAndLivenessProbes() {
            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            var container = sts.getSpec().getTemplate().getSpec().getContainers().get(0);
            assertThat(container.getReadinessProbe()).isNotNull();
            assertThat(container.getReadinessProbe().getExec().getCommand()).contains("mongosh");
            assertThat(container.getLivenessProbe()).isNotNull();
            assertThat(container.getLivenessProbe().getExec().getCommand()).contains("mongosh");
        }

        @Test
        void shouldHaveVolumeClaimWith20Gi() {
            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            assertThat(sts.getSpec().getVolumeClaimTemplates()).hasSize(1);
            var pvc = sts.getSpec().getVolumeClaimTemplates().get(0);
            assertThat(pvc.getSpec().getResources().getRequests().get("storage").toString())
                    .isEqualTo("20Gi");
        }

        @Test
        void shouldSetCustomStorageClass() {
            eddi.getSpec().getDatastore().getManaged().getStorage().setStorageClassName("fast-ssd");
            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            var pvc = sts.getSpec().getVolumeClaimTemplates().get(0);
            assertThat(pvc.getSpec().getStorageClassName()).isEqualTo("fast-ssd");
        }

        @Test
        void shouldHaveRestrictedSecurityContext() {
            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            var sc = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getSecurityContext();
            assertThat(sc.getRunAsNonRoot()).isTrue();
            assertThat(sc.getAllowPrivilegeEscalation()).isFalse();
            assertThat(sc.getCapabilities().getDrop()).contains("ALL");
        }

        @Test
        void shouldHaveStandardLabels() {
            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            assertThat(sts.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/name", "eddi")
                    .containsEntry("app.kubernetes.io/instance", CR_NAME)
                    .containsEntry("app.kubernetes.io/component", "mongodb")
                    .containsEntry("app.kubernetes.io/managed-by", "eddi-operator");
        }

        @Test
        void shouldBeHeadlessStatefulSet() {
            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            assertThat(sts.getSpec().getServiceName()).isEqualTo(CR_NAME + "-mongodb");
            assertThat(sts.getSpec().getReplicas()).isEqualTo(1);
        }

        @Test
        void shouldMountDataVolume() {
            var sts = callDesired(new MongoStatefulSetDR(), eddi);
            var mounts = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
            assertThat(mounts).anyMatch(m -> "data".equals(m.getName()) && "/data/db".equals(m.getMountPath()));
        }
    }

    // ────────────────────────────────────────────────────
    //  PostgresStatefulSetDR
    // ────────────────────────────────────────────────────

    @Nested
    class PostgresStatefulSet {

        @BeforeEach
        void enableManagedPostgres() {
            eddi.getSpec().getDatastore().setType(DatastoreType.POSTGRES);
            eddi.getSpec().getDatastore().getManaged().setEnabled(true);
        }

        @Test
        void shouldHaveCorrectNameAndNamespace() {
            var sts = callDesired(new PostgresStatefulSetDR(), eddi);
            assertThat(sts.getMetadata().getName()).isEqualTo(CR_NAME + "-postgres");
            assertThat(sts.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
        }

        @Test
        void shouldDefaultToPostgres16AlpineImage() {
            var sts = callDesired(new PostgresStatefulSetDR(), eddi);
            var image = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getImage();
            assertThat(image).isEqualTo("postgres:16-alpine");
        }

        @Test
        void shouldExposePort5432() {
            var sts = callDesired(new PostgresStatefulSetDR(), eddi);
            var ports = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getPorts();
            assertThat(ports).hasSize(1);
            assertThat(ports.get(0).getContainerPort()).isEqualTo(5432);
        }

        @Test
        void shouldReferenceCredentialSecret() {
            var sts = callDesired(new PostgresStatefulSetDR(), eddi);
            var envVars = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
            var pgUser = envVars.stream()
                    .filter(e -> "POSTGRES_USER".equals(e.getName()))
                    .findFirst().orElseThrow();
            assertThat(pgUser.getValueFrom().getSecretKeyRef().getName())
                    .isEqualTo(CR_NAME + "-postgres-credentials");
            assertThat(pgUser.getValueFrom().getSecretKeyRef().getKey()).isEqualTo("username");
        }

        @Test
        void shouldSetPgdataEnvVar() {
            var sts = callDesired(new PostgresStatefulSetDR(), eddi);
            var envVars = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
            assertThat(envVars).anyMatch(e ->
                    "PGDATA".equals(e.getName())
                            && "/var/lib/postgresql/data/pgdata".equals(e.getValue()));
        }

        @Test
        void shouldHaveReadinessProbeWithPgIsready() {
            var sts = callDesired(new PostgresStatefulSetDR(), eddi);
            var probe = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getReadinessProbe();
            assertThat(probe.getExec().getCommand()).contains("pg_isready");
        }

        @Test
        void shouldMountDataVolume() {
            var sts = callDesired(new PostgresStatefulSetDR(), eddi);
            var mounts = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
            assertThat(mounts).anyMatch(m ->
                    "data".equals(m.getName()) && "/var/lib/postgresql/data".equals(m.getMountPath()));
        }
    }

    // ────────────────────────────────────────────────────
    //  MongoServiceDR
    // ────────────────────────────────────────────────────

    @Nested
    class MongoService {

        @Test
        void shouldBeHeadlessServiceOnPort27017() {
            var svc = callDesired(new MongoServiceDR(), eddi);
            assertThat(svc.getMetadata().getName()).isEqualTo(CR_NAME + "-mongodb");
            assertThat(svc.getSpec().getClusterIP()).isEqualTo("None");
            assertThat(svc.getSpec().getPorts()).hasSize(1);
            assertThat(svc.getSpec().getPorts().get(0).getPort()).isEqualTo(27017);
        }

        @Test
        void shouldSelectMongodbComponent() {
            var svc = callDesired(new MongoServiceDR(), eddi);
            assertThat(svc.getSpec().getSelector())
                    .containsEntry("app.kubernetes.io/component", "mongodb");
        }
    }

    // ────────────────────────────────────────────────────
    //  PostgresServiceDR
    // ────────────────────────────────────────────────────

    @Nested
    class PostgresService {

        @Test
        void shouldBeHeadlessServiceOnPort5432() {
            var svc = callDesired(new PostgresServiceDR(), eddi);
            assertThat(svc.getMetadata().getName()).isEqualTo(CR_NAME + "-postgres");
            assertThat(svc.getSpec().getClusterIP()).isEqualTo("None");
            assertThat(svc.getSpec().getPorts()).hasSize(1);
            assertThat(svc.getSpec().getPorts().get(0).getPort()).isEqualTo(5432);
        }

        @Test
        void shouldSelectPostgresComponent() {
            var svc = callDesired(new PostgresServiceDR(), eddi);
            assertThat(svc.getSpec().getSelector())
                    .containsEntry("app.kubernetes.io/component", "postgres");
        }
    }
}
