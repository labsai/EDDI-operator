package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.crd.spec.BackupStorageType;
import ai.labs.eddi.operator.reconciler.EddiReconciler;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Edge-case unit tests for EddiReconciler.validateSpec().
 * Complements the validation tests in EnterpriseHardeningTests
 * and ValidationAndSecurityTests with additional edge cases.
 */
class ReconcilerValidationTest {

    // ────────────────────────────────────────────────────
    //  Replica Validation
    // ────────────────────────────────────────────────────

    @Nested
    class ReplicaValidation {

        @Test
        void shouldRejectZeroReplicas() {
            var spec = new EddiSpec();
            spec.setReplicas(0);
            assertThat(EddiReconciler.validateSpec(spec))
                    .contains("replicas")
                    .contains(">= 1");
        }

        @Test
        void shouldRejectNegativeReplicas() {
            var spec = new EddiSpec();
            spec.setReplicas(-1);
            assertThat(EddiReconciler.validateSpec(spec))
                    .contains("replicas");
        }

        @Test
        void shouldAcceptOneReplica() {
            var spec = new EddiSpec();
            spec.setReplicas(1);
            assertThat(EddiReconciler.validateSpec(spec)).isNull();
        }

        @Test
        void shouldAcceptMultipleReplicas() {
            var spec = new EddiSpec();
            spec.setReplicas(5);
            assertThat(EddiReconciler.validateSpec(spec)).isNull();
        }
    }

    // ────────────────────────────────────────────────────
    //  Backup Validation Edge Cases
    // ────────────────────────────────────────────────────

    @Nested
    class BackupValidation {

        @Test
        void shouldRejectNullSchedule() {
            var spec = new EddiSpec();
            spec.getBackup().setEnabled(true);
            spec.getBackup().setSchedule(null);
            assertThat(EddiReconciler.validateSpec(spec))
                    .contains("backup.schedule");
        }

        @Test
        void shouldRejectWhitespaceOnlySchedule() {
            var spec = new EddiSpec();
            spec.getBackup().setEnabled(true);
            spec.getBackup().setSchedule("   ");
            assertThat(EddiReconciler.validateSpec(spec))
                    .contains("backup.schedule");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "not-a-cron",
                "* * *",           // Only 3 fields
                "* * * *",         // Only 4 fields
                "* * * * * *"      // 6 fields (standard cron has 5)
        })
        void shouldRejectInvalidCronFormats(String schedule) {
            var spec = new EddiSpec();
            spec.getBackup().setEnabled(true);
            spec.getBackup().setSchedule(schedule);
            assertThat(EddiReconciler.validateSpec(spec))
                    .contains("5-field cron");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "0 2 * * *",           // simple daily
                "*/15 0-6 1,15 * 1-5", // complex
                "0 0 * * 0",           // weekly
                "@reboot"              // special — hmm actually this won't match 5-field pattern
        })
        void shouldAcceptValidCronFormats(String schedule) {
            var spec = new EddiSpec();
            spec.getBackup().setEnabled(true);
            spec.getBackup().setSchedule(schedule);
            spec.getBackup().getStorage().setType(BackupStorageType.PVC);
            // @reboot is special — skip assertion if it doesn't match
            if (schedule.startsWith("@")) {
                // @reboot is not a 5-field format, so it should be rejected
                assertThat(EddiReconciler.validateSpec(spec)).isNotNull();
            } else {
                assertThat(EddiReconciler.validateSpec(spec)).isNull();
            }
        }

        @Test
        void shouldRejectS3WithNullBucket() {
            var spec = new EddiSpec();
            spec.getBackup().setEnabled(true);
            spec.getBackup().setSchedule("0 2 * * *");
            spec.getBackup().getStorage().setType(BackupStorageType.S3);
            spec.getBackup().getStorage().getS3().setBucket(null);
            assertThat(EddiReconciler.validateSpec(spec))
                    .contains("bucket")
                    .contains("required");
        }

        @Test
        void shouldNotValidateScheduleWhenBackupDisabled() {
            var spec = new EddiSpec();
            spec.getBackup().setEnabled(false);
            spec.getBackup().setSchedule("not-a-cron");
            assertThat(EddiReconciler.validateSpec(spec)).isNull();
        }
    }

    // ────────────────────────────────────────────────────
    //  Default Spec Validation
    // ────────────────────────────────────────────────────

    @Nested
    class DefaultSpec {

        @Test
        void defaultSpecShouldBeValid() {
            assertThat(EddiReconciler.validateSpec(new EddiSpec())).isNull();
        }
    }
}
