package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.BackupStorageType;
import ai.labs.eddi.operator.dependent.core.EddiServiceDR;
import ai.labs.eddi.operator.dependent.core.ServiceAccountDR;
import ai.labs.eddi.operator.dependent.core.VaultSecretDR;
import ai.labs.eddi.operator.dependent.extras.HpaDR;
import ai.labs.eddi.operator.dependent.extras.NetworkPolicyDR;
import ai.labs.eddi.operator.dependent.extras.PdbDR;
import ai.labs.eddi.operator.dependent.lifecycle.BackupPvcDR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static ai.labs.eddi.operator.unit.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for core & extras dependent resources:
 * ServiceAccountDR, EddiServiceDR, VaultSecretDR (static helpers only),
 * HpaDR, PdbDR, NetworkPolicyDR, BackupPvcDR.
 */
class CoreDRTest {

    private EddiResource eddi;

    @BeforeEach
    void setUp() {
        eddi = createEddi();
    }

    // ────────────────────────────────────────────────────
    //  ServiceAccountDR
    // ────────────────────────────────────────────────────

    @Nested
    class ServiceAccount {

        @Test
        void shouldHaveCorrectNameAndNamespace() {
            var sa = callDesired(new ServiceAccountDR(), eddi);
            assertThat(sa.getMetadata().getName()).isEqualTo(CR_NAME + "-server");
            assertThat(sa.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
        }

        @Test
        void shouldHaveStandardLabels() {
            var sa = callDesired(new ServiceAccountDR(), eddi);
            assertThat(sa.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/name", "eddi")
                    .containsEntry("app.kubernetes.io/component", "server")
                    .containsEntry("app.kubernetes.io/managed-by", "eddi-operator");
        }
    }

    // ────────────────────────────────────────────────────
    //  EddiServiceDR
    // ────────────────────────────────────────────────────

    @Nested
    class EddiService {

        @Test
        void shouldBeClusterIPServiceOnPort8080() {
            var svc = callDesired(new EddiServiceDR(), eddi);
            assertThat(svc.getMetadata().getName()).isEqualTo(CR_NAME + "-server");
            assertThat(svc.getSpec().getType()).isEqualTo("ClusterIP");
            assertThat(svc.getSpec().getPorts()).hasSize(1);
            assertThat(svc.getSpec().getPorts().get(0).getPort()).isEqualTo(8080);
            assertThat(svc.getSpec().getPorts().get(0).getName()).isEqualTo("http");
        }

        @Test
        void shouldSelectServerComponent() {
            var svc = callDesired(new EddiServiceDR(), eddi);
            assertThat(svc.getSpec().getSelector())
                    .containsEntry("app.kubernetes.io/component", "server")
                    .containsEntry("app.kubernetes.io/instance", CR_NAME);
        }
    }

    // ────────────────────────────────────────────────────
    //  VaultSecretDR — static helpers only
    //  (desired() reads from context — tested in integration)
    // ────────────────────────────────────────────────────

    @Nested
    class VaultSecret {

        @Test
        void shouldGenerateBase64EncodedKey() {
            var key = VaultSecretDR.generateRandomKey();
            assertThat(key).isNotBlank();
            // Base64 of 32 bytes = 44 characters
            assertThat(key).hasSize(44);
        }

        @Test
        void shouldGenerateUniqueKeys() {
            var k1 = VaultSecretDR.generateRandomKey();
            var k2 = VaultSecretDR.generateRandomKey();
            assertThat(k1).isNotEqualTo(k2);
        }
    }

    // ────────────────────────────────────────────────────
    //  HpaDR
    // ────────────────────────────────────────────────────

    @Nested
    class Hpa {

        @BeforeEach
        void enableHpa() {
            eddi.getSpec().getAutoscaling().setEnabled(true);
        }

        @Test
        void shouldHaveCorrectName() {
            var hpa = callDesired(new HpaDR(), eddi);
            assertThat(hpa.getMetadata().getName()).isEqualTo(CR_NAME + "-hpa");
        }

        @Test
        void shouldTargetServerDeployment() {
            var hpa = callDesired(new HpaDR(), eddi);
            var ref = hpa.getSpec().getScaleTargetRef();
            assertThat(ref.getApiVersion()).isEqualTo("apps/v1");
            assertThat(ref.getKind()).isEqualTo("Deployment");
            assertThat(ref.getName()).isEqualTo(CR_NAME + "-server");
        }

        @Test
        void shouldHaveDefaultReplicaRange() {
            var hpa = callDesired(new HpaDR(), eddi);
            assertThat(hpa.getSpec().getMinReplicas()).isEqualTo(2);
            assertThat(hpa.getSpec().getMaxReplicas()).isEqualTo(10);
        }

        @Test
        void shouldHaveCpuAndMemoryMetrics() {
            var hpa = callDesired(new HpaDR(), eddi);
            assertThat(hpa.getSpec().getMetrics()).hasSize(2);
            var metricNames = hpa.getSpec().getMetrics().stream()
                    .map(m -> m.getResource().getName()).toList();
            assertThat(metricNames).containsExactly("cpu", "memory");
        }

        @Test
        void shouldUseConfiguredTargetUtilization() {
            eddi.getSpec().getAutoscaling().setTargetCPU(50);
            eddi.getSpec().getAutoscaling().setMinReplicas(3);
            eddi.getSpec().getAutoscaling().setMaxReplicas(20);

            var hpa = callDesired(new HpaDR(), eddi);
            assertThat(hpa.getSpec().getMinReplicas()).isEqualTo(3);
            assertThat(hpa.getSpec().getMaxReplicas()).isEqualTo(20);
            assertThat(hpa.getSpec().getMetrics().get(0).getResource().getTarget()
                    .getAverageUtilization()).isEqualTo(50);
        }
    }

    // ────────────────────────────────────────────────────
    //  PdbDR
    // ────────────────────────────────────────────────────

    @Nested
    class Pdb {

        @BeforeEach
        void enablePdb() {
            eddi.getSpec().getPodDisruptionBudget().setEnabled(true);
        }

        @Test
        void shouldHaveCorrectName() {
            var pdb = callDesired(new PdbDR(), eddi);
            assertThat(pdb.getMetadata().getName()).isEqualTo(CR_NAME + "-pdb");
        }

        @Test
        void shouldDefaultToOneMinAvailable() {
            var pdb = callDesired(new PdbDR(), eddi);
            assertThat(pdb.getSpec().getMinAvailable().getIntVal()).isEqualTo(1);
        }

        @Test
        void shouldSelectServerPods() {
            var pdb = callDesired(new PdbDR(), eddi);
            assertThat(pdb.getSpec().getSelector().getMatchLabels())
                    .containsEntry("app.kubernetes.io/component", "server");
        }

        @Test
        void shouldUseCustomMinAvailable() {
            eddi.getSpec().getPodDisruptionBudget().setMinAvailable(2);
            var pdb = callDesired(new PdbDR(), eddi);
            assertThat(pdb.getSpec().getMinAvailable().getIntVal()).isEqualTo(2);
        }
    }

    // ────────────────────────────────────────────────────
    //  NetworkPolicyDR
    // ────────────────────────────────────────────────────

    @Nested
    class NetworkPolicy {

        @Test
        void shouldHaveCorrectName() {
            var np = callDesired(new NetworkPolicyDR(), eddi);
            assertThat(np.getMetadata().getName()).isEqualTo(CR_NAME + "-network-policy");
        }

        @Test
        void shouldSelectServerPods() {
            var np = callDesired(new NetworkPolicyDR(), eddi);
            assertThat(np.getSpec().getPodSelector().getMatchLabels())
                    .containsEntry("app.kubernetes.io/component", "server");
        }

        @Test
        void shouldHaveIngressPolicyType() {
            var np = callDesired(new NetworkPolicyDR(), eddi);
            assertThat(np.getSpec().getPolicyTypes()).contains("Ingress");
        }

        @Test
        void shouldAllowPort8080() {
            var np = callDesired(new NetworkPolicyDR(), eddi);
            var ingress = np.getSpec().getIngress().get(0);
            assertThat(ingress.getPorts().get(0).getPort().getIntVal()).isEqualTo(8080);
            assertThat(ingress.getPorts().get(0).getProtocol()).isEqualTo("TCP");
        }
    }

    // ────────────────────────────────────────────────────
    //  BackupPvcDR
    // ────────────────────────────────────────────────────

    @Nested
    class BackupPvc {

        @BeforeEach
        void enableBackup() {
            eddi.getSpec().getBackup().setEnabled(true);
            eddi.getSpec().getBackup().setSchedule("0 2 * * *");
            eddi.getSpec().getBackup().getStorage().setType(BackupStorageType.PVC);
        }

        @Test
        void shouldHaveCorrectName() {
            var pvc = callDesired(new BackupPvcDR(), eddi);
            assertThat(pvc.getMetadata().getName()).isEqualTo(CR_NAME + "-backup-pvc");
        }

        @Test
        void shouldHaveReadWriteOnceAccess() {
            var pvc = callDesired(new BackupPvcDR(), eddi);
            assertThat(pvc.getSpec().getAccessModes()).contains("ReadWriteOnce");
        }

        @Test
        void shouldHaveBackupLabels() {
            var pvc = callDesired(new BackupPvcDR(), eddi);
            assertThat(pvc.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/component", "backup");
        }
    }
}
