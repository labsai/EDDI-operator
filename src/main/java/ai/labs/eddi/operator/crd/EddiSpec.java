package ai.labs.eddi.operator.crd;

import ai.labs.eddi.operator.crd.spec.*;
import ai.labs.eddi.operator.crd.spec.PvcRetentionPolicy;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Top-level spec for the Eddi custom resource.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EddiSpec {

    @JsonPropertyDescription("EDDI version — maps to the container image tag")
    private String version = "6.0.0";

    @JsonPropertyDescription("Number of EDDI server replicas")
    private int replicas = 1;

    @JsonPropertyDescription("Container image configuration")
    private ImageSpec image = new ImageSpec();

    @JsonPropertyDescription("Datastore configuration (MongoDB or PostgreSQL)")
    private DatastoreSpec datastore = new DatastoreSpec();

    @JsonPropertyDescription("Messaging configuration (in-memory or NATS)")
    private MessagingSpec messaging = new MessagingSpec();

    @JsonPropertyDescription("Vault / secrets configuration")
    private VaultSpec vault = new VaultSpec();

    @JsonPropertyDescription("Authentication configuration (Keycloak)")
    private AuthSpec auth = new AuthSpec();

    @JsonPropertyDescription("Network exposure configuration (Route, Ingress, or auto-detect)")
    private ExposureSpec exposure = new ExposureSpec();

    @JsonPropertyDescription("Manager UI configuration")
    private ManagerSpec manager = new ManagerSpec();

    @JsonPropertyDescription("Monitoring configuration (ServiceMonitor, Grafana, Alerts)")
    private MonitoringSpec monitoring = new MonitoringSpec();

    @JsonPropertyDescription("Autoscaling configuration (HPA)")
    private AutoscalingSpec autoscaling = new AutoscalingSpec();

    @JsonPropertyDescription("Pod Disruption Budget configuration")
    private PdbSpec podDisruptionBudget = new PdbSpec();

    @JsonPropertyDescription("EDDI server resource requests and limits")
    private ResourcesSpec resources = new ResourcesSpec();

    @JsonPropertyDescription("Backup and restore configuration")
    private BackupSpec backup = new BackupSpec();

    @JsonPropertyDescription("Pod scheduling constraints (nodeSelector, tolerations, affinity, topologySpreadConstraints)")
    private SchedulingSpec scheduling = new SchedulingSpec();

    @JsonPropertyDescription("Additional labels applied to EDDI server pods")
    private Map<String, String> podLabels = new LinkedHashMap<>();

    @JsonPropertyDescription("Additional annotations applied to EDDI server pods (e.g., for service mesh sidecar injection)")
    private Map<String, String> podAnnotations = new LinkedHashMap<>();

    @JsonPropertyDescription("PVC retention policy on CR deletion: Retain (default) or Delete")
    private PvcRetentionPolicy pvcRetentionPolicy = PvcRetentionPolicy.RETAIN;

    @JsonPropertyDescription("CORS allowed origins")
    private String cors = "http://localhost:3000,http://localhost:7070";

    @JsonPropertyDescription("Whether to create a NetworkPolicy")
    private boolean networkPolicy = false;

    // --- Getters and Setters ---

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getReplicas() {
        return replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public ImageSpec getImage() {
        return image;
    }

    public void setImage(ImageSpec image) {
        this.image = image;
    }

    public DatastoreSpec getDatastore() {
        return datastore;
    }

    public void setDatastore(DatastoreSpec datastore) {
        this.datastore = datastore;
    }

    public MessagingSpec getMessaging() {
        return messaging;
    }

    public void setMessaging(MessagingSpec messaging) {
        this.messaging = messaging;
    }

    public VaultSpec getVault() {
        return vault;
    }

    public void setVault(VaultSpec vault) {
        this.vault = vault;
    }

    public AuthSpec getAuth() {
        return auth;
    }

    public void setAuth(AuthSpec auth) {
        this.auth = auth;
    }

    public ExposureSpec getExposure() {
        return exposure;
    }

    public void setExposure(ExposureSpec exposure) {
        this.exposure = exposure;
    }

    public ManagerSpec getManager() {
        return manager;
    }

    public void setManager(ManagerSpec manager) {
        this.manager = manager;
    }

    public MonitoringSpec getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(MonitoringSpec monitoring) {
        this.monitoring = monitoring;
    }

    public AutoscalingSpec getAutoscaling() {
        return autoscaling;
    }

    public void setAutoscaling(AutoscalingSpec autoscaling) {
        this.autoscaling = autoscaling;
    }

    public PdbSpec getPodDisruptionBudget() {
        return podDisruptionBudget;
    }

    public void setPodDisruptionBudget(PdbSpec podDisruptionBudget) {
        this.podDisruptionBudget = podDisruptionBudget;
    }

    public ResourcesSpec getResources() {
        return resources;
    }

    public void setResources(ResourcesSpec resources) {
        this.resources = resources;
    }

    public BackupSpec getBackup() {
        return backup;
    }

    public void setBackup(BackupSpec backup) {
        this.backup = backup;
    }

    public SchedulingSpec getScheduling() {
        return scheduling;
    }

    public void setScheduling(SchedulingSpec scheduling) {
        this.scheduling = scheduling;
    }

    public Map<String, String> getPodLabels() {
        return podLabels;
    }

    public void setPodLabels(Map<String, String> podLabels) {
        this.podLabels = podLabels;
    }

    public Map<String, String> getPodAnnotations() {
        return podAnnotations;
    }

    public void setPodAnnotations(Map<String, String> podAnnotations) {
        this.podAnnotations = podAnnotations;
    }

    public PvcRetentionPolicy getPvcRetentionPolicy() {
        return pvcRetentionPolicy;
    }

    public void setPvcRetentionPolicy(PvcRetentionPolicy pvcRetentionPolicy) {
        this.pvcRetentionPolicy = pvcRetentionPolicy;
    }

    public String getCors() {
        return cors;
    }

    public void setCors(String cors) {
        this.cors = cors;
    }

    public boolean isNetworkPolicy() {
        return networkPolicy;
    }

    public void setNetworkPolicy(boolean networkPolicy) {
        this.networkPolicy = networkPolicy;
    }
}
