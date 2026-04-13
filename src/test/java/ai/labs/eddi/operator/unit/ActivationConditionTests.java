package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.conditions.*;
import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.*;
import ai.labs.eddi.operator.util.OpenShiftDetector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static ai.labs.eddi.operator.unit.TestSupport.createEddi;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exhaustive unit tests for all 20 activation conditions.
 * <p>
 * Conditions that only read from EddiSpec are tested with null context/dependentResource.
 * IngressActivationCondition and RouteActivationCondition depend on OpenShiftDetector,
 * which caches its result in a static field — we pre-set it via the public reset() method.
 * </p>
 */
class ActivationConditionTests {

    private EddiResource eddi;

    @BeforeEach
    void setUp() {
        eddi = createEddi();
    }

    // ────────────────────────────────────────────────────
    //  Datastore Conditions
    // ────────────────────────────────────────────────────

    @Nested
    class MongoActivation {

        @Test
        void shouldActivateForManagedMongodb() {
            eddi.getSpec().getDatastore().setType(DatastoreType.MONGODB);
            eddi.getSpec().getDatastore().getManaged().setEnabled(true);
            assertThat(new MongoActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateForPostgres() {
            eddi.getSpec().getDatastore().setType(DatastoreType.POSTGRES);
            eddi.getSpec().getDatastore().getManaged().setEnabled(true);
            assertThat(new MongoActivationCondition().isMet(null, eddi, null)).isFalse();
        }

        @Test
        void shouldNotActivateWhenManagedDisabled() {
            eddi.getSpec().getDatastore().setType(DatastoreType.MONGODB);
            eddi.getSpec().getDatastore().getManaged().setEnabled(false);
            assertThat(new MongoActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    @Nested
    class PostgresActivation {

        @Test
        void shouldActivateForManagedPostgres() {
            eddi.getSpec().getDatastore().setType(DatastoreType.POSTGRES);
            eddi.getSpec().getDatastore().getManaged().setEnabled(true);
            assertThat(new PostgresActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateForMongodb() {
            eddi.getSpec().getDatastore().setType(DatastoreType.MONGODB);
            eddi.getSpec().getDatastore().getManaged().setEnabled(true);
            assertThat(new PostgresActivationCondition().isMet(null, eddi, null)).isFalse();
        }

        @Test
        void shouldNotActivateWhenManagedDisabled() {
            eddi.getSpec().getDatastore().setType(DatastoreType.POSTGRES);
            eddi.getSpec().getDatastore().getManaged().setEnabled(false);
            assertThat(new PostgresActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    // ────────────────────────────────────────────────────
    //  Messaging Conditions
    // ────────────────────────────────────────────────────

    @Nested
    class NatsActivation {

        @Test
        void shouldActivateForManagedNats() {
            eddi.getSpec().getMessaging().setType(MessagingType.NATS);
            eddi.getSpec().getMessaging().getManaged().setEnabled(true);
            assertThat(new NatsActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateForInMemory() {
            // default messaging type is in-memory
            assertThat(new NatsActivationCondition().isMet(null, eddi, null)).isFalse();
        }

        @Test
        void shouldNotActivateWhenManagedDisabled() {
            eddi.getSpec().getMessaging().setType(MessagingType.NATS);
            eddi.getSpec().getMessaging().getManaged().setEnabled(false);
            assertThat(new NatsActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    // ────────────────────────────────────────────────────
    //  Vault Condition
    // ────────────────────────────────────────────────────

    @Nested
    class VaultSecretActivation {

        @Test
        void shouldActivateWhenSecretRefIsEmpty() {
            eddi.getSpec().getVault().setMasterKeySecretRef("");
            assertThat(new VaultSecretActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldActivateWhenSecretRefIsNull() {
            eddi.getSpec().getVault().setMasterKeySecretRef(null);
            assertThat(new VaultSecretActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateWhenSecretRefIsProvided() {
            eddi.getSpec().getVault().setMasterKeySecretRef("my-vault-secret");
            assertThat(new VaultSecretActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    // ────────────────────────────────────────────────────
    //  Auth Conditions
    // ────────────────────────────────────────────────────

    @Nested
    class ManagedAuthActivation {

        @Test
        void shouldActivateWhenBothEnabled() {
            eddi.getSpec().getAuth().setEnabled(true);
            eddi.getSpec().getAuth().getManaged().setEnabled(true);
            assertThat(new ManagedAuthActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateWhenAuthDisabled() {
            eddi.getSpec().getAuth().setEnabled(false);
            eddi.getSpec().getAuth().getManaged().setEnabled(true);
            assertThat(new ManagedAuthActivationCondition().isMet(null, eddi, null)).isFalse();
        }

        @Test
        void shouldNotActivateWhenManagedDisabled() {
            eddi.getSpec().getAuth().setEnabled(true);
            eddi.getSpec().getAuth().getManaged().setEnabled(false);
            assertThat(new ManagedAuthActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    @Nested
    class KeycloakSecretActivation {

        @Test
        void shouldActivateWhenManagedAuthWithNoExternalRef() {
            eddi.getSpec().getAuth().setEnabled(true);
            eddi.getSpec().getAuth().getManaged().setEnabled(true);
            eddi.getSpec().getAuth().getManaged().setAdminSecretRef("");
            assertThat(new KeycloakSecretActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldActivateWhenAdminSecretRefIsNull() {
            eddi.getSpec().getAuth().setEnabled(true);
            eddi.getSpec().getAuth().getManaged().setEnabled(true);
            eddi.getSpec().getAuth().getManaged().setAdminSecretRef(null);
            assertThat(new KeycloakSecretActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateWhenExternalRefProvided() {
            eddi.getSpec().getAuth().setEnabled(true);
            eddi.getSpec().getAuth().getManaged().setEnabled(true);
            eddi.getSpec().getAuth().getManaged().setAdminSecretRef("my-keycloak-creds");
            assertThat(new KeycloakSecretActivationCondition().isMet(null, eddi, null)).isFalse();
        }

        @Test
        void shouldNotActivateWhenAuthDisabled() {
            eddi.getSpec().getAuth().setEnabled(false);
            assertThat(new KeycloakSecretActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    // ────────────────────────────────────────────────────
    //  Extras Conditions
    // ────────────────────────────────────────────────────

    @Nested
    class HpaActivation {

        @Test
        void shouldActivateWhenEnabled() {
            eddi.getSpec().getAutoscaling().setEnabled(true);
            assertThat(new HpaActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateByDefault() {
            assertThat(new HpaActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    @Nested
    class PdbActivation {

        @Test
        void shouldActivateWhenEnabled() {
            eddi.getSpec().getPodDisruptionBudget().setEnabled(true);
            assertThat(new PdbActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateByDefault() {
            assertThat(new PdbActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    @Nested
    class NetworkPolicyActivation {

        @Test
        void shouldActivateWhenEnabled() {
            eddi.getSpec().setNetworkPolicy(true);
            assertThat(new NetworkPolicyActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateByDefault() {
            assertThat(new NetworkPolicyActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    @Nested
    class ManagerActivation {

        @Test
        void shouldActivateWhenEnabled() {
            eddi.getSpec().getManager().setEnabled(true);
            assertThat(new ManagerActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateByDefault() {
            assertThat(new ManagerActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    // ────────────────────────────────────────────────────
    //  Monitoring Conditions
    // ────────────────────────────────────────────────────

    @Nested
    class ServiceMonitorActivation {

        @Test
        void shouldActivateWhenEnabled() {
            eddi.getSpec().getMonitoring().getServiceMonitor().setEnabled(true);
            assertThat(new ServiceMonitorActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateByDefault() {
            assertThat(new ServiceMonitorActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    @Nested
    class GrafanaDashboardActivation {

        @Test
        void shouldActivateWhenEnabled() {
            eddi.getSpec().getMonitoring().getGrafanaDashboard().setEnabled(true);
            assertThat(new GrafanaDashboardActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateByDefault() {
            assertThat(new GrafanaDashboardActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    @Nested
    class AlertsActivation {

        @Test
        void shouldActivateWhenEnabled() {
            eddi.getSpec().getMonitoring().getAlerts().setEnabled(true);
            assertThat(new AlertsActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateByDefault() {
            assertThat(new AlertsActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    // ────────────────────────────────────────────────────
    //  Backup Conditions (already partially tested in
    //  EnterpriseHardeningTests — included here for completeness)
    // ────────────────────────────────────────────────────

    @Nested
    class BackupActivation {

        @Test
        void shouldActivateWhenEnabled() {
            eddi.getSpec().getBackup().setEnabled(true);
            assertThat(new BackupActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateWhenDisabled() {
            assertThat(new BackupActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    @Nested
    class BackupPvcActivation {

        @Test
        void shouldActivateForPvcStorage() {
            eddi.getSpec().getBackup().setEnabled(true);
            eddi.getSpec().getBackup().getStorage().setType(BackupStorageType.PVC);
            assertThat(new BackupPvcActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateForS3Storage() {
            eddi.getSpec().getBackup().setEnabled(true);
            eddi.getSpec().getBackup().getStorage().setType(BackupStorageType.S3);
            assertThat(new BackupPvcActivationCondition().isMet(null, eddi, null)).isFalse();
        }

        @Test
        void shouldNotActivateWhenBackupDisabled() {
            eddi.getSpec().getBackup().getStorage().setType(BackupStorageType.PVC);
            assertThat(new BackupPvcActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    // ────────────────────────────────────────────────────
    //  Exposure Conditions (depend on OpenShiftDetector)
    //  We pre-set the static cache to avoid needing a real Context.
    // ────────────────────────────────────────────────────

    @Nested
    class IngressActivation {

        @AfterEach
        void resetOpenShiftCache() {
            OpenShiftDetector.reset();
        }

        @Test
        void shouldActivateForExplicitIngress() {
            eddi.getSpec().getExposure().setType(ExposureType.INGRESS);
            assertThat(new IngressActivationCondition().isMet(null, eddi, null)).isTrue();
        }

        @Test
        void shouldNotActivateForNone() {
            eddi.getSpec().getExposure().setType(ExposureType.NONE);
            assertThat(new IngressActivationCondition().isMet(null, eddi, null)).isFalse();
        }

        @Test
        void shouldNotActivateForExplicitRoute() {
            eddi.getSpec().getExposure().setType(ExposureType.ROUTE);
            assertThat(new IngressActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }

    @Nested
    class RouteActivation {

        @AfterEach
        void resetOpenShiftCache() {
            OpenShiftDetector.reset();
        }

        @Test
        void shouldNotActivateForNone() {
            eddi.getSpec().getExposure().setType(ExposureType.NONE);
            assertThat(new RouteActivationCondition().isMet(null, eddi, null)).isFalse();
        }

        @Test
        void shouldNotActivateForExplicitIngress() {
            eddi.getSpec().getExposure().setType(ExposureType.INGRESS);
            assertThat(new RouteActivationCondition().isMet(null, eddi, null)).isFalse();
        }
    }
}
