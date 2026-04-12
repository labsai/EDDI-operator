package ai.labs.eddi.operator.dependent.lifecycle;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.BackupSpec;
import ai.labs.eddi.operator.crd.spec.BackupStorageType;
import ai.labs.eddi.operator.crd.spec.DatastoreType;
import ai.labs.eddi.operator.util.Defaults;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.batch.v1.CronJob;
import io.fabric8.kubernetes.api.model.batch.v1.CronJobBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages a CronJob that performs scheduled backups of the EDDI datastore.
 * Supports MongoDB (mongodump) and PostgreSQL (pg_dump) to PVC or S3 storage.
 * Activated when spec.backup.enabled=true.
 */
@KubernetesDependent
public class BackupCronJobDR extends CRUDKubernetesDependentResource<CronJob, EddiResource> {

    private static final String DEFAULT_MONGO_BACKUP_IMAGE = "mongo:7.0";
    private static final String DEFAULT_POSTGRES_BACKUP_IMAGE = "postgres:16-alpine";

    public BackupCronJobDR() {
        super(CronJob.class);
    }

    @Override
    protected CronJob desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var backup = spec.getBackup();
        var name = Labels.resourceName(eddi, "backup");
        var datastoreType = spec.getDatastore().getType();
        var isPvcStorage = BackupStorageType.PVC == backup.getStorage().getType();

        // Resolve the host name for the datastore — sanitized against shell injection
        var dbHost = Labels.sanitizeForShell(
                DatastoreType.MONGODB == datastoreType
                        ? Labels.resourceName(eddi, "mongodb")
                        : Labels.resourceName(eddi, "postgres")
        );

        // Volumes and mounts — only for PVC-based storage
        var volumes = new ArrayList<Volume>();
        var volumeMounts = new ArrayList<VolumeMount>();

        if (isPvcStorage) {
            volumes.add(new VolumeBuilder()
                    .withName("backup-storage")
                    .withNewPersistentVolumeClaim()
                        .withClaimName(Labels.resourceName(eddi, "backup-pvc"))
                    .endPersistentVolumeClaim()
                    .build());
            volumeMounts.add(new VolumeMountBuilder()
                    .withName("backup-storage")
                    .withMountPath("/backup")
                    .build());
        }

        // Build backup command based on datastore type and storage type
        var command = buildBackupCommand(datastoreType, dbHost, backup);

        // Environment variables
        var env = new ArrayList<EnvVar>();

        if (!isPvcStorage) {
            // S3 environment variables
            var s3 = backup.getStorage().getS3();
            env.add(new EnvVarBuilder().withName("S3_BUCKET").withValue(s3.getBucket()).build());
            env.add(new EnvVarBuilder().withName("S3_REGION").withValue(s3.getRegion()).build());
            if (s3.getEndpoint() != null && !s3.getEndpoint().isBlank()) {
                env.add(new EnvVarBuilder().withName("S3_ENDPOINT").withValue(s3.getEndpoint()).build());
            }
            if (s3.getSecretRef() != null && !s3.getSecretRef().isBlank()) {
                env.add(new EnvVarBuilder()
                        .withName("AWS_ACCESS_KEY_ID")
                        .withNewValueFrom()
                            .withNewSecretKeyRef()
                                .withName(s3.getSecretRef())
                                .withKey("access-key")
                            .endSecretKeyRef()
                        .endValueFrom()
                        .build());
                env.add(new EnvVarBuilder()
                        .withName("AWS_SECRET_ACCESS_KEY")
                        .withNewValueFrom()
                            .withNewSecretKeyRef()
                                .withName(s3.getSecretRef())
                                .withKey("secret-key")
                            .endSecretKeyRef()
                        .endValueFrom()
                        .build());
            }
        }

        // For managed postgres, mount credentials
        if (DatastoreType.POSTGRES == datastoreType && spec.getDatastore().getManaged().isEnabled()) {
            var pgSecretName = Labels.resourceName(eddi, "postgres-credentials");
            env.add(new EnvVarBuilder()
                    .withName("PGUSER")
                    .withNewValueFrom()
                        .withNewSecretKeyRef()
                            .withName(pgSecretName)
                            .withKey("username")
                        .endSecretKeyRef()
                    .endValueFrom()
                    .build());
            env.add(new EnvVarBuilder()
                    .withName("PGPASSWORD")
                    .withNewValueFrom()
                        .withNewSecretKeyRef()
                            .withName(pgSecretName)
                            .withKey("password")
                        .endSecretKeyRef()
                    .endValueFrom()
                    .build());
        }

        // Resolve backup image — configurable via spec, with sensible defaults
        var backupImage = resolveBackupImage(backup, datastoreType);

        return new CronJobBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "backup"))
                .endMetadata()
                .withNewSpec()
                    .withSchedule(backup.getSchedule())
                    .withConcurrencyPolicy("Forbid")
                    .withSuccessfulJobsHistoryLimit(3)
                    .withFailedJobsHistoryLimit(3)
                    .withNewJobTemplate()
                        .withNewSpec()
                            .withBackoffLimit(2)
                            .withNewTemplate()
                                .withNewMetadata()
                                    .withLabels(Labels.standard(eddi, "backup"))
                                .endMetadata()
                                .withNewSpec()
                                    .withRestartPolicy("OnFailure")
                                    .addNewContainer()
                                        .withName("backup")
                                        .withImage(backupImage)
                                        .withCommand("/bin/sh", "-c")
                                        .withArgs(command)
                                        .withEnv(env)
                                        .withVolumeMounts(volumeMounts)
                                        .withSecurityContext(Defaults.restrictedSecurityContext())
                                        .withNewResources()
                                            .withRequests(Map.of(
                                                    "cpu", new Quantity("100m"),
                                                    "memory", new Quantity("256Mi")))
                                            .withLimits(Map.of(
                                                    "cpu", new Quantity("500m"),
                                                    "memory", new Quantity("512Mi")))
                                        .endResources()
                                    .endContainer()
                                    .withVolumes(volumes)
                                .endSpec()
                            .endTemplate()
                        .endSpec()
                    .endJobTemplate()
                .endSpec()
                .build();
    }

    /**
     * Resolves the backup container image, using the configured image spec
     * if provided, or falling back to datastore-appropriate defaults.
     */
    public static String resolveBackupImage(BackupSpec backup, DatastoreType datastoreType) {
        var imgSpec = backup.getImage();
        if (imgSpec != null) {
            var repo = imgSpec.getRepository();
            var tag = imgSpec.getTag();
            if (repo != null && !repo.isBlank()) {
                return Defaults.resolveImage(repo, tag);
            }
        }
        return DatastoreType.MONGODB == datastoreType
                ? DEFAULT_MONGO_BACKUP_IMAGE
                : DEFAULT_POSTGRES_BACKUP_IMAGE;
    }

    /**
     * Builds the backup shell command for the specified datastore type.
     * Includes timestamp-based naming and retention cleanup.
     * The dbHost parameter MUST be sanitized via Labels.sanitizeForShell() before calling.
     *
     * @param datastoreType the datastore type enum
     * @param dbHost        sanitized hostname for the database service
     * @param backup        backup configuration spec
     */
    public static String buildBackupCommand(DatastoreType datastoreType, String dbHost, BackupSpec backup) {
        var retention = backup.getRetentionDays();
        var isS3 = BackupStorageType.S3 == backup.getStorage().getType();

        if (DatastoreType.MONGODB == datastoreType) {
            if (isS3) {
                return "mongodump --host=" + dbHost + " --port=27017 --db=eddi --archive"
                        + " | gzip | aws s3 cp - s3://$S3_BUCKET/eddi-backups/eddi-$(date +%Y%m%d-%H%M%S).archive.gz"
                        + " --endpoint-url=${S3_ENDPOINT:-https://s3.amazonaws.com}";
            } else {
                return "mongodump --host=" + dbHost + " --port=27017 --db=eddi"
                        + " --archive=/backup/eddi-$(date +%Y%m%d-%H%M%S).archive"
                        + " && find /backup -name '*.archive' -mtime +" + retention + " -delete";
            }
        } else {
            if (isS3) {
                return "pg_dump -h " + dbHost + " -p 5432 -U $PGUSER -d eddi -Fc"
                        + " | aws s3 cp - s3://$S3_BUCKET/eddi-backups/eddi-$(date +%Y%m%d-%H%M%S).dump"
                        + " --endpoint-url=${S3_ENDPOINT:-https://s3.amazonaws.com}";
            } else {
                return "pg_dump -h " + dbHost + " -p 5432 -U $PGUSER -d eddi -Fc"
                        + " -f /backup/eddi-$(date +%Y%m%d-%H%M%S).dump"
                        + " && find /backup -name '*.dump' -mtime +" + retention + " -delete";
            }
        }
    }
}
