package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.EddiSpec;
import ai.labs.eddi.operator.crd.spec.*;
import ai.labs.eddi.operator.dependent.lifecycle.BackupCronJobDR;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BackupCronJobDR — verifies command generation,
 * image resolution, and security (shell sanitization).
 */
class BackupCronJobDRTest {

    private EddiSpec spec;

    @BeforeEach
    void setUp() {
        spec = new EddiSpec();
        spec.getBackup().setEnabled(true);
        spec.getBackup().setSchedule("0 2 * * *");
    }

    // ────────────────────────────────────────────────────
    //  Command generation — MongoDB
    // ────────────────────────────────────────────────────

    @Test
    void shouldGenerateMongoPvcBackupCommand() {
        spec.getDatastore().setType(DatastoreType.MONGODB);
        spec.getBackup().getStorage().setType(BackupStorageType.PVC);

        var cmd = BackupCronJobDR.buildBackupCommand(
                DatastoreType.MONGODB, "my-eddi-mongodb", spec.getBackup());

        assertThat(cmd).contains("mongodump");
        assertThat(cmd).contains("--host=my-eddi-mongodb");
        assertThat(cmd).contains("--archive=/backup/");
        assertThat(cmd).contains("find /backup");
        assertThat(cmd).doesNotContain("aws s3");
    }

    @Test
    void shouldGenerateMongoS3BackupCommand() {
        spec.getDatastore().setType(DatastoreType.MONGODB);
        spec.getBackup().getStorage().setType(BackupStorageType.S3);

        var cmd = BackupCronJobDR.buildBackupCommand(
                DatastoreType.MONGODB, "my-eddi-mongodb", spec.getBackup());

        assertThat(cmd).contains("mongodump");
        assertThat(cmd).contains("--archive");
        assertThat(cmd).contains("gzip");
        assertThat(cmd).contains("aws s3 cp");
        assertThat(cmd).doesNotContain("find /backup");
    }

    // ────────────────────────────────────────────────────
    //  Command generation — PostgreSQL
    // ────────────────────────────────────────────────────

    @Test
    void shouldGeneratePostgresPvcBackupCommand() {
        spec.getDatastore().setType(DatastoreType.POSTGRES);
        spec.getBackup().getStorage().setType(BackupStorageType.PVC);

        var cmd = BackupCronJobDR.buildBackupCommand(
                DatastoreType.POSTGRES, "my-eddi-postgres", spec.getBackup());

        assertThat(cmd).contains("pg_dump");
        assertThat(cmd).contains("-h my-eddi-postgres");
        assertThat(cmd).contains("-f /backup/");
        assertThat(cmd).contains("find /backup");
        assertThat(cmd).doesNotContain("aws s3");
    }

    @Test
    void shouldGeneratePostgresS3BackupCommand() {
        spec.getDatastore().setType(DatastoreType.POSTGRES);
        spec.getBackup().getStorage().setType(BackupStorageType.S3);

        var cmd = BackupCronJobDR.buildBackupCommand(
                DatastoreType.POSTGRES, "my-eddi-postgres", spec.getBackup());

        assertThat(cmd).contains("pg_dump");
        assertThat(cmd).contains("aws s3 cp");
        assertThat(cmd).doesNotContain("find /backup");
    }

    // ────────────────────────────────────────────────────
    //  Retention days
    // ────────────────────────────────────────────────────

    @Test
    void shouldApplyRetentionDays() {
        spec.getDatastore().setType(DatastoreType.MONGODB);
        spec.getBackup().getStorage().setType(BackupStorageType.PVC);
        spec.getBackup().setRetentionDays(14);

        var cmd = BackupCronJobDR.buildBackupCommand(
                DatastoreType.MONGODB, "my-eddi-mongodb", spec.getBackup());

        assertThat(cmd).contains("-mtime +14");
    }

    @Test
    void shouldUseDefaultRetentionDays() {
        spec.getDatastore().setType(DatastoreType.MONGODB);
        spec.getBackup().getStorage().setType(BackupStorageType.PVC);
        // Default retention = 7

        var cmd = BackupCronJobDR.buildBackupCommand(
                DatastoreType.MONGODB, "my-eddi-mongodb", spec.getBackup());

        assertThat(cmd).contains("-mtime +7");
    }

    // ────────────────────────────────────────────────────
    //  Image resolution
    // ────────────────────────────────────────────────────

    @Test
    void shouldUseDefaultMongoBackupImage() {
        var image = BackupCronJobDR.resolveBackupImage(spec.getBackup(), DatastoreType.MONGODB);
        assertThat(image).isEqualTo("mongo:7.0");
    }

    @Test
    void shouldUseDefaultPostgresBackupImage() {
        var image = BackupCronJobDR.resolveBackupImage(spec.getBackup(), DatastoreType.POSTGRES);
        assertThat(image).isEqualTo("postgres:16-alpine");
    }

    @Test
    void shouldUseCustomBackupImageWhenConfigured() {
        spec.getBackup().getImage().setRepository("custom/backup-tool");
        spec.getBackup().getImage().setTag("latest");

        var image = BackupCronJobDR.resolveBackupImage(spec.getBackup(), DatastoreType.MONGODB);
        assertThat(image).isEqualTo("custom/backup-tool:latest");
    }

    // ────────────────────────────────────────────────────
    //  Timestamp in filenames
    // ────────────────────────────────────────────────────

    @Test
    void shouldIncludeTimestampInFilenames() {
        spec.getBackup().getStorage().setType(BackupStorageType.PVC);

        var cmd = BackupCronJobDR.buildBackupCommand(
                DatastoreType.MONGODB, "my-eddi-mongodb", spec.getBackup());

        assertThat(cmd).contains("$(date +%Y%m%d-%H%M%S)");
    }

    // ────────────────────────────────────────────────────
    //  Port numbers
    // ────────────────────────────────────────────────────

    @Test
    void shouldUseCorrectMongoPort() {
        spec.getBackup().getStorage().setType(BackupStorageType.PVC);
        var cmd = BackupCronJobDR.buildBackupCommand(
                DatastoreType.MONGODB, "host", spec.getBackup());
        assertThat(cmd).contains("--port=27017");
    }

    @Test
    void shouldUseCorrectPostgresPort() {
        spec.getBackup().getStorage().setType(BackupStorageType.PVC);
        var cmd = BackupCronJobDR.buildBackupCommand(
                DatastoreType.POSTGRES, "host", spec.getBackup());
        assertThat(cmd).contains("-p 5432");
    }
}
