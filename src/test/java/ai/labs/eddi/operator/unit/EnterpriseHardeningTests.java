package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.conditions.BackupActivationCondition;
import ai.labs.eddi.operator.conditions.BackupPvcActivationCondition;
import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.crd.spec.SchedulingSpec;
import ai.labs.eddi.operator.reconciler.EddiReconciler;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Toleration;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for enterprise hardening features:
 * - Pod scheduling controls
 * - Custom labels/annotations
 * - PVC retention policy validation
 * - Backup spec validation
 * - Upgrade phase detection
 */
class EnterpriseHardeningTests {

    // ────────────────────────────────────────────────────
    //  SchedulingSpec
    // ────────────────────────────────────────────────────

    @Test
    void shouldDefaultToEmptyScheduling() {
        var sched = new SchedulingSpec();
        assertThat(sched.getNodeSelector()).isEmpty();
        assertThat(sched.getTolerations()).isEmpty();
        assertThat(sched.getAffinity()).isNull();
        assertThat(sched.getTopologySpreadConstraints()).isEmpty();
    }

    @Test
    void shouldAcceptNodeSelector() {
        var sched = new SchedulingSpec();
        sched.setNodeSelector(Map.of("kubernetes.io/arch", "amd64"));
        assertThat(sched.getNodeSelector()).containsEntry("kubernetes.io/arch", "amd64");
    }

    @Test
    void shouldAcceptTolerations() {
        var sched = new SchedulingSpec();
        var toleration = new Toleration();
        toleration.setKey("dedicated");
        toleration.setOperator("Equal");
        toleration.setValue("ai");
        toleration.setEffect("NoSchedule");
        sched.setTolerations(List.of(toleration));
        assertThat(sched.getTolerations()).hasSize(1);
        assertThat(sched.getTolerations().get(0).getKey()).isEqualTo("dedicated");
    }

    // ────────────────────────────────────────────────────
    //  EddiSpec — new fields
    // ────────────────────────────────────────────────────

    @Test
    void shouldDefaultPodLabelsAndAnnotationsToEmpty() {
        var spec = new EddiSpec();
        assertThat(spec.getPodLabels()).isEmpty();
        assertThat(spec.getPodAnnotations()).isEmpty();
    }

    @Test
    void shouldDefaultPvcRetentionPolicyToRetain() {
        var spec = new EddiSpec();
        assertThat(spec.getPvcRetentionPolicy()).isEqualTo("Retain");
    }

    @Test
    void shouldDefaultBackupToDisabled() {
        var spec = new EddiSpec();
        assertThat(spec.getBackup().isEnabled()).isFalse();
    }

    @Test
    void shouldDefaultSchedulingToEmpty() {
        var spec = new EddiSpec();
        assertThat(spec.getScheduling()).isNotNull();
        assertThat(spec.getScheduling().getNodeSelector()).isEmpty();
    }

    // ────────────────────────────────────────────────────
    //  PVC Retention Policy Validation
    // ────────────────────────────────────────────────────

    @Test
    void shouldAcceptRetainPolicy() {
        var spec = new EddiSpec();
        spec.setPvcRetentionPolicy("Retain");
        assertThat(EddiReconciler.validateSpec(spec)).isNull();
    }

    @Test
    void shouldAcceptDeletePolicy() {
        var spec = new EddiSpec();
        spec.setPvcRetentionPolicy("Delete");
        assertThat(EddiReconciler.validateSpec(spec)).isNull();
    }

    @Test
    void shouldRejectInvalidPvcRetentionPolicy() {
        var spec = new EddiSpec();
        spec.setPvcRetentionPolicy("garbage");
        assertThat(EddiReconciler.validateSpec(spec))
                .contains("pvcRetentionPolicy")
                .contains("garbage");
    }

    // ────────────────────────────────────────────────────
    //  Backup Spec Validation
    // ────────────────────────────────────────────────────

    @Test
    void shouldAcceptDisabledBackup() {
        var spec = new EddiSpec();
        spec.getBackup().setEnabled(false);
        assertThat(EddiReconciler.validateSpec(spec)).isNull();
    }

    @Test
    void shouldAcceptEnabledBackupWithPvc() {
        var spec = new EddiSpec();
        spec.getBackup().setEnabled(true);
        spec.getBackup().getStorage().setType("pvc");
        assertThat(EddiReconciler.validateSpec(spec)).isNull();
    }

    @Test
    void shouldAcceptEnabledBackupWithS3() {
        var spec = new EddiSpec();
        spec.getBackup().setEnabled(true);
        spec.getBackup().getStorage().setType("s3");
        spec.getBackup().getStorage().getS3().setBucket("my-bucket");
        assertThat(EddiReconciler.validateSpec(spec)).isNull();
    }

    @Test
    void shouldRejectS3BackupWithoutBucket() {
        var spec = new EddiSpec();
        spec.getBackup().setEnabled(true);
        spec.getBackup().getStorage().setType("s3");
        spec.getBackup().getStorage().getS3().setBucket("");
        assertThat(EddiReconciler.validateSpec(spec))
                .contains("bucket")
                .contains("required");
    }

    @Test
    void shouldRejectInvalidBackupStorageType() {
        var spec = new EddiSpec();
        spec.getBackup().setEnabled(true);
        spec.getBackup().getStorage().setType("gcs");
        assertThat(EddiReconciler.validateSpec(spec))
                .contains("backup.storage.type")
                .contains("gcs");
    }

    @Test
    void shouldRejectInvalidCronSchedule() {
        var spec = new EddiSpec();
        spec.getBackup().setEnabled(true);
        spec.getBackup().setSchedule("not a cron");
        assertThat(EddiReconciler.validateSpec(spec))
                .contains("backup.schedule")
                .contains("5-field cron");
    }

    @Test
    void shouldRejectEmptyCronSchedule() {
        var spec = new EddiSpec();
        spec.getBackup().setEnabled(true);
        spec.getBackup().setSchedule("");
        assertThat(EddiReconciler.validateSpec(spec))
                .contains("backup.schedule");
    }

    @Test
    void shouldAcceptValidCronSchedule() {
        var spec = new EddiSpec();
        spec.getBackup().setEnabled(true);
        spec.getBackup().setSchedule("0 2 * * *");
        spec.getBackup().getStorage().setType("pvc");
        assertThat(EddiReconciler.validateSpec(spec)).isNull();
    }

    @Test
    void shouldAcceptComplexCronSchedule() {
        var spec = new EddiSpec();
        spec.getBackup().setEnabled(true);
        spec.getBackup().setSchedule("*/15 0-6 1,15 * 1-5");
        spec.getBackup().getStorage().setType("pvc");
        assertThat(EddiReconciler.validateSpec(spec)).isNull();
    }

    // ────────────────────────────────────────────────────
    //  Backup Activation Conditions
    // ────────────────────────────────────────────────────

    @Test
    void backupConditionShouldBeActiveWhenEnabled() {
        var cond = new BackupActivationCondition();
        var eddi = createEddi();
        eddi.getSpec().getBackup().setEnabled(true);
        assertThat(cond.isMet(null, eddi, null)).isTrue();
    }

    @Test
    void backupConditionShouldBeInactiveWhenDisabled() {
        var cond = new BackupActivationCondition();
        var eddi = createEddi();
        eddi.getSpec().getBackup().setEnabled(false);
        assertThat(cond.isMet(null, eddi, null)).isFalse();
    }

    @Test
    void backupPvcConditionShouldBeActiveWhenPvc() {
        var cond = new BackupPvcActivationCondition();
        var eddi = createEddi();
        eddi.getSpec().getBackup().setEnabled(true);
        eddi.getSpec().getBackup().getStorage().setType("pvc");
        assertThat(cond.isMet(null, eddi, null)).isTrue();
    }

    @Test
    void backupPvcConditionShouldBeInactiveWhenS3() {
        var cond = new BackupPvcActivationCondition();
        var eddi = createEddi();
        eddi.getSpec().getBackup().setEnabled(true);
        eddi.getSpec().getBackup().getStorage().setType("s3");
        assertThat(cond.isMet(null, eddi, null)).isFalse();
    }

    @Test
    void backupPvcConditionShouldBeInactiveWhenBackupDisabled() {
        var cond = new BackupPvcActivationCondition();
        var eddi = createEddi();
        eddi.getSpec().getBackup().setEnabled(false);
        eddi.getSpec().getBackup().getStorage().setType("pvc");
        assertThat(cond.isMet(null, eddi, null)).isFalse();
    }

    // ────────────────────────────────────────────────────
    //  Custom Pod Labels
    // ────────────────────────────────────────────────────

    @Test
    void shouldStorePodLabels() {
        var spec = new EddiSpec();
        spec.setPodLabels(Map.of("cost-center", "ai-team", "env", "production"));
        assertThat(spec.getPodLabels()).hasSize(2);
        assertThat(spec.getPodLabels()).containsEntry("cost-center", "ai-team");
    }

    @Test
    void shouldStorePodAnnotations() {
        var spec = new EddiSpec();
        spec.setPodAnnotations(Map.of("sidecar.istio.io/inject", "true"));
        assertThat(spec.getPodAnnotations()).hasSize(1);
        assertThat(spec.getPodAnnotations()).containsEntry("sidecar.istio.io/inject", "true");
    }

    // ────────────────────────────────────────────────────
    //  Helpers
    // ────────────────────────────────────────────────────

    private EddiResource createEddi() {
        var eddi = new EddiResource();
        var meta = new ObjectMeta();
        meta.setName("test-eddi");
        meta.setNamespace("test-ns");
        eddi.setMetadata(meta);
        eddi.setSpec(new EddiSpec());
        return eddi;
    }
}
