package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.crd.spec.DatastoreType;
import ai.labs.eddi.operator.crd.spec.KeycloakMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for HPA, PDB, and related EddiSpec defaults.
 * Verifies the structure and default values of autoscaling and
 * disruption budget specs.
 */
class ExtrasDRTest {

    @Test
    void hpaShouldDefaultToDisabled() {
        var spec = new EddiSpec();
        assertThat(spec.getAutoscaling().isEnabled()).isFalse();
    }

    @Test
    void hpaShouldHaveReasonableDefaults() {
        var spec = new EddiSpec();
        assertThat(spec.getAutoscaling().getMinReplicas()).isEqualTo(2);
        assertThat(spec.getAutoscaling().getMaxReplicas()).isEqualTo(10);
        assertThat(spec.getAutoscaling().getTargetCPU()).isEqualTo(70);
        assertThat(spec.getAutoscaling().getTargetMemory()).isEqualTo(80);
    }

    @Test
    void pdbShouldDefaultToDisabled() {
        var spec = new EddiSpec();
        assertThat(spec.getPodDisruptionBudget().isEnabled()).isFalse();
    }

    @Test
    void pdbShouldDefaultToOneMinAvailable() {
        var spec = new EddiSpec();
        assertThat(spec.getPodDisruptionBudget().getMinAvailable()).isEqualTo(1);
    }

    @Test
    void monitoringShouldDefaultToDisabled() {
        var spec = new EddiSpec();
        assertThat(spec.getMonitoring().getServiceMonitor().isEnabled()).isFalse();
        assertThat(spec.getMonitoring().getGrafanaDashboard().isEnabled()).isFalse();
        assertThat(spec.getMonitoring().getAlerts().isEnabled()).isFalse();
    }

    @Test
    void authShouldDefaultToDisabled() {
        var spec = new EddiSpec();
        assertThat(spec.getAuth().isEnabled()).isFalse();
        assertThat(spec.getAuth().getManaged().isEnabled()).isFalse();
    }

    @Test
    void keycloakModeShouldDefaultToDev() {
        var spec = new EddiSpec();
        assertThat(spec.getAuth().getManaged().getMode()).isEqualTo(KeycloakMode.DEV);
    }

    @Test
    void managerShouldDefaultToDisabled() {
        var spec = new EddiSpec();
        assertThat(spec.getManager().isEnabled()).isFalse();
    }

    @Test
    void vaultSecretRefShouldDefaultToEmpty() {
        var spec = new EddiSpec();
        assertThat(spec.getVault().getMasterKeySecretRef()).isEmpty();
    }

    @Test
    void resourcesShouldHaveDefaultValues() {
        var spec = new EddiSpec();
        assertThat(spec.getResources().getRequests().getCpu()).isEqualTo("250m");
        assertThat(spec.getResources().getRequests().getMemory()).isEqualTo("384Mi");
        assertThat(spec.getResources().getLimits().getCpu()).isEqualTo("2");
        assertThat(spec.getResources().getLimits().getMemory()).isEqualTo("1Gi");
    }

    @Test
    void defaultReplicasShouldBeOne() {
        var spec = new EddiSpec();
        assertThat(spec.getReplicas()).isEqualTo(1);
    }

    @Test
    void defaultVersionShouldBe600() {
        var spec = new EddiSpec();
        assertThat(spec.getVersion()).isEqualTo("6.0.0");
    }

    @Test
    void storageDefaultsShouldBeReasonable() {
        var spec = new EddiSpec();
        spec.getDatastore().setType(DatastoreType.MONGODB);
        assertThat(spec.getDatastore().getManaged().getStorage().getSize()).isEqualTo("20Gi");
    }
}
