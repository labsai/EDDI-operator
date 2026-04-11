package ai.labs.eddi.operator.dependent.datastore;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.util.Defaults;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import java.util.List;
import java.util.Map;

/**
 * Manages a single-node PostgreSQL 16 StatefulSet for development/demo use.
 * Activated only when spec.datastore.type=postgres and spec.datastore.managed.enabled=true.
 */
@KubernetesDependent
public class PostgresStatefulSetDR extends CRUDKubernetesDependentResource<StatefulSet, EddiResource> {

    public PostgresStatefulSetDR() {
        super(StatefulSet.class);
    }

    @Override
    protected StatefulSet desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var managed = spec.getDatastore().getManaged();
        var name = Labels.resourceName(eddi, "postgres");
        var credentialSecretName = Labels.resourceName(eddi, "postgres-credentials");

        var resources = Defaults.buildResources(managed.getResources());

        var imgSpec = managed.getImage();
        var image = Defaults.resolveImage(
                (imgSpec.getRepository() == null || imgSpec.getRepository().isBlank()) ? "postgres" : imgSpec.getRepository(),
                (imgSpec.getTag() == null || imgSpec.getTag().isBlank()) ? "16-alpine" : imgSpec.getTag()
        );

        var storageSize = managed.getStorage().getSize();
        var storageClassName = managed.getStorage().getStorageClassName();

        var pvcSpecBuilder = new PersistentVolumeClaimSpecBuilder()
                .withAccessModes("ReadWriteOnce")
                .withNewResources()
                    .withRequests(Map.of("storage", new Quantity(storageSize)))
                .endResources();

        if (storageClassName != null && !storageClassName.isBlank()) {
            pvcSpecBuilder.withStorageClassName(storageClassName);
        }

        var pvc = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                    .withName("data")
                    .withLabels(Labels.standard(eddi, "postgres"))
                .endMetadata()
                .withSpec(pvcSpecBuilder.build())
                .build();

        return new StatefulSetBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "postgres"))
                .endMetadata()
                .withNewSpec()
                    .withReplicas(1)
                    .withServiceName(name)
                    .withNewSelector()
                        .withMatchLabels(Labels.selector(eddi, "postgres"))
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(Labels.standard(eddi, "postgres"))
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("postgres")
                                .withImage(image)
                                .withPorts(List.of(
                                        new ContainerPortBuilder()
                                                .withName("postgres")
                                                .withContainerPort(5432)
                                                .build()
                                ))
                                .withResources(resources)
                                .withSecurityContext(Defaults.restrictedSecurityContext())
                                .withEnv(List.of(
                                        new EnvVarBuilder()
                                                .withName("POSTGRES_DB")
                                                .withValue("eddi")
                                                .build(),
                                        new EnvVarBuilder()
                                                .withName("POSTGRES_USER")
                                                .withNewValueFrom()
                                                    .withNewSecretKeyRef()
                                                        .withName(credentialSecretName)
                                                        .withKey("username")
                                                    .endSecretKeyRef()
                                                .endValueFrom()
                                                .build(),
                                        new EnvVarBuilder()
                                                .withName("POSTGRES_PASSWORD")
                                                .withNewValueFrom()
                                                    .withNewSecretKeyRef()
                                                        .withName(credentialSecretName)
                                                        .withKey("password")
                                                    .endSecretKeyRef()
                                                .endValueFrom()
                                                .build(),
                                        new EnvVarBuilder()
                                                .withName("PGDATA")
                                                .withValue("/var/lib/postgresql/data/pgdata")
                                                .build()
                                ))
                                .addNewVolumeMount()
                                    .withName("data")
                                    .withMountPath("/var/lib/postgresql/data")
                                .endVolumeMount()
                                .withNewReadinessProbe()
                                    .withNewExec()
                                        .withCommand("pg_isready", "-U", "eddi")
                                    .endExec()
                                    .withInitialDelaySeconds(10)
                                    .withPeriodSeconds(10)
                                .endReadinessProbe()
                                .withNewLivenessProbe()
                                    .withNewExec()
                                        .withCommand("pg_isready", "-U", "eddi")
                                    .endExec()
                                    .withInitialDelaySeconds(30)
                                    .withPeriodSeconds(15)
                                    .withFailureThreshold(3)
                                .endLivenessProbe()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                    .withVolumeClaimTemplates(pvc)
                .endSpec()
                .build();
    }
}
