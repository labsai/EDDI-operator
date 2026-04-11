package ai.labs.eddi.operator.dependent.core;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.ResourcesSpec;
import ai.labs.eddi.operator.util.Defaults;
import ai.labs.eddi.operator.util.Labels;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the core EDDI server Deployment.
 * Depends on ConfigMap, VaultSecret, and database readiness.
 */
@KubernetesDependent
public class EddiDeploymentDR extends CRUDKubernetesDependentResource<Deployment, EddiResource> {

    private static final Logger LOG = Logger.getLogger(EddiDeploymentDR.class);

    public EddiDeploymentDR() {
        super(Deployment.class);
    }

    @Override
    protected Deployment desired(EddiResource eddi, Context<EddiResource> context) {
        var spec = eddi.getSpec();
        var name = Labels.resourceName(eddi, "server");
        var configMapName = Labels.resourceName(eddi, "config");
        var vaultSecretName = resolveVaultSecretName(eddi);

        // Pod annotations — include config hash for rollout on config change
        var annotations = new LinkedHashMap<String, String>();
        // Merge user-provided pod annotations first, then operator annotations
        if (spec.getPodAnnotations() != null) {
            annotations.putAll(spec.getPodAnnotations());
        }
        annotations.put("eddi.labs.ai/config-hash", computeConfigHash(eddi, context));

        // Environment variables from ConfigMap
        var envFrom = new ArrayList<EnvFromSource>();
        envFrom.add(new EnvFromSourceBuilder()
                .withNewConfigMapRef()
                    .withName(configMapName)
                .endConfigMapRef()
                .build());

        // Mount vault secret as env
        var env = new ArrayList<EnvVar>();
        env.add(new EnvVarBuilder()
                .withName("EDDI_VAULT_MASTER_KEY")
                .withNewValueFrom()
                    .withNewSecretKeyRef()
                        .withName(vaultSecretName)
                        .withKey("master-key")
                        .withOptional(false)
                    .endSecretKeyRef()
                .endValueFrom()
                .build());

        // If external postgres with secretRef, mount credentials
        if ("postgres".equals(spec.getDatastore().getType())
                && !spec.getDatastore().getManaged().isEnabled()) {
            var secretRef = spec.getDatastore().getExternal().getSecretRef();
            if (secretRef != null && !secretRef.isBlank()) {
                env.add(envFromSecret(secretRef, "username", "QUARKUS_DATASOURCE_USERNAME"));
                env.add(envFromSecret(secretRef, "password", "QUARKUS_DATASOURCE_PASSWORD"));
                env.add(envFromSecret(secretRef, "jdbc-url", "QUARKUS_DATASOURCE_JDBC_URL"));
            }
        }

        // If managed postgres, mount auto-generated credentials
        if ("postgres".equals(spec.getDatastore().getType())
                && spec.getDatastore().getManaged().isEnabled()) {
            var pgSecretName = Labels.resourceName(eddi, "postgres-credentials");
            env.add(envFromSecret(pgSecretName, "username", "QUARKUS_DATASOURCE_USERNAME"));
            env.add(envFromSecret(pgSecretName, "password", "QUARKUS_DATASOURCE_PASSWORD"));
        }

        // Image pull secrets
        var pullSecrets = new ArrayList<LocalObjectReference>();
        for (var s : spec.getImage().getPullSecrets()) {
            pullSecrets.add(new LocalObjectReferenceBuilder().withName(s).build());
        }

        // Resource requirements
        var resources = Defaults.buildResources(spec.getResources());

        return new DeploymentBuilder()
                .withNewMetadata()
                    .withName(name)
                    .withNamespace(eddi.getMetadata().getNamespace())
                    .withLabels(Labels.standard(eddi, "server"))
                .endMetadata()
                .withNewSpec()
                    .withReplicas(spec.getReplicas())
                    .withNewSelector()
                        .withMatchLabels(Labels.selector(eddi, "server"))
                    .endSelector()
                    .withNewStrategy()
                        .withType("RollingUpdate")
                        .withNewRollingUpdate()
                            .withMaxSurge(new IntOrString(1))
                            .withMaxUnavailable(new IntOrString(0))
                        .endRollingUpdate()
                    .endStrategy()
                    .withNewTemplate()
                        .withNewMetadata()
                            .withLabels(mergedPodLabels(eddi))
                            .withAnnotations(annotations)
                        .endMetadata()
                        .withNewSpec()
                            .withServiceAccountName(Labels.resourceName(eddi, "server"))
                            .withImagePullSecrets(pullSecrets)
                            .withNodeSelector(nullIfEmpty(spec.getScheduling().getNodeSelector()))
                            .withTolerations(nullIfEmptyList(spec.getScheduling().getTolerations()))
                            .withAffinity(spec.getScheduling().getAffinity())
                            .withTopologySpreadConstraints(nullIfEmptyList(spec.getScheduling().getTopologySpreadConstraints()))
                            .addNewContainer()
                                .withName("eddi")
                                .withImage(Defaults.resolveEddiImage(spec))
                                .withImagePullPolicy(spec.getImage().getPullPolicy())
                                .withPorts(List.of(
                                        new ContainerPortBuilder()
                                                .withName("http")
                                                .withContainerPort(8080)
                                                .withProtocol("TCP")
                                                .build()
                                ))
                                .withEnvFrom(envFrom)
                                .withEnv(env)
                                .withResources(resources)
                                .withSecurityContext(Defaults.restrictedSecurityContext())
                                .withNewReadinessProbe()
                                    .withNewHttpGet()
                                        .withPath("/q/health/ready")
                                        .withPort(new IntOrString(8080))
                                    .endHttpGet()
                                    .withInitialDelaySeconds(10)
                                    .withPeriodSeconds(10)
                                    .withFailureThreshold(3)
                                .endReadinessProbe()
                                .withNewLivenessProbe()
                                    .withNewHttpGet()
                                        .withPath("/q/health/live")
                                        .withPort(new IntOrString(8080))
                                    .endHttpGet()
                                    .withInitialDelaySeconds(30)
                                    .withPeriodSeconds(15)
                                    .withFailureThreshold(3)
                                .endLivenessProbe()
                                .withNewStartupProbe()
                                    .withNewHttpGet()
                                        .withPath("/q/health/started")
                                        .withPort(new IntOrString(8080))
                                    .endHttpGet()
                                    .withInitialDelaySeconds(5)
                                    .withPeriodSeconds(5)
                                    .withFailureThreshold(30)
                                .endStartupProbe()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }

    private String resolveVaultSecretName(EddiResource eddi) {
        var ref = eddi.getSpec().getVault().getMasterKeySecretRef();
        return (ref != null && !ref.isBlank()) ? ref : Labels.resourceName(eddi, "vault-key");
    }

    private String computeConfigHash(EddiResource eddi, Context<EddiResource> context) {
        var configMapName = Labels.resourceName(eddi, "config");
        // Use direct client lookup to avoid secondary resource ambiguity
        try {
            var configMap = context.getClient().configMaps()
                    .inNamespace(eddi.getMetadata().getNamespace())
                    .withName(configMapName)
                    .get();
            if (configMap != null && configMap.getData() != null) {
                return ai.labs.eddi.operator.util.Hashing.hash(configMap.getData());
            }
        } catch (Exception e) {
            LOG.debugf(e, "ConfigMap '%s' not available yet, computing hash from spec", configMapName);
        }
        // Fallback: compute from spec
        var data = ConfigMapDR.buildConfigData(eddi.getSpec(), eddi.getMetadata().getName());
        return ai.labs.eddi.operator.util.Hashing.hash(data);
    }

    private EnvVar envFromSecret(String secretName, String key, String envName) {
        return new EnvVarBuilder()
                .withName(envName)
                .withNewValueFrom()
                    .withNewSecretKeyRef()
                        .withName(secretName)
                        .withKey(key)
                        .withOptional(true)
                    .endSecretKeyRef()
                .endValueFrom()
                .build();
    }

    /**
     * Merges standard labels with user-specified podLabels.
     * User labels are added first so operator labels take precedence.
     */
    private Map<String, String> mergedPodLabels(EddiResource eddi) {
        var labels = new LinkedHashMap<String, String>();
        if (eddi.getSpec().getPodLabels() != null) {
            labels.putAll(eddi.getSpec().getPodLabels());
        }
        labels.putAll(Labels.standard(eddi, "server"));
        return labels;
    }

    /**
     * Returns null for empty maps so Fabric8 omits the field from the YAML.
     */
    private static <K, V> Map<K, V> nullIfEmpty(Map<K, V> map) {
        return (map == null || map.isEmpty()) ? null : map;
    }

    /**
     * Returns null for empty lists so Fabric8 omits the field from the YAML.
     */
    private static <T> List<T> nullIfEmptyList(List<T> list) {
        return (list == null || list.isEmpty()) ? null : list;
    }
}
