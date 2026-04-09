package ai.labs.eddi.operator.crd;

import io.fabric8.kubernetes.api.model.Condition;
import java.util.ArrayList;
import java.util.List;

/**
 * Status subresource for the Eddi custom resource.
 * Follows Kubernetes conventions with conditions array + human-readable phase.
 */
public class EddiStatus {

    private Long observedGeneration;


    /**
     * Human-readable phase: Pending, Deploying, Running, Failed, Upgrading
     */
    private String phase = "Pending";

    /**
     * Currently running EDDI version
     */
    private String version;

    /**
     * Desired replica count
     */
    private int replicas;

    /**
     * Number of ready replicas
     */
    private int readyReplicas;

    /**
     * Resolved external URL (from Route or Ingress)
     */
    private String url;

    /**
     * Kubernetes-standard conditions for programmatic use
     */
    private List<Condition> conditions = new ArrayList<>();

    // --- Getters and Setters ---

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

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

    public int getReadyReplicas() {
        return readyReplicas;
    }

    public void setReadyReplicas(int readyReplicas) {
        this.readyReplicas = readyReplicas;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    public Long getObservedGeneration() {
        return observedGeneration;
    }

    public void setObservedGeneration(Long observedGeneration) {
        this.observedGeneration = observedGeneration;
    }
}
