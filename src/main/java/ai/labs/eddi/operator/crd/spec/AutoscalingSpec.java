package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * HPA autoscaling configuration.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AutoscalingSpec {

    private boolean enabled = false;
    private int minReplicas = 2;
    private int maxReplicas = 10;
    private int targetCPU = 70;
    private int targetMemory = 80;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMinReplicas() {
        return minReplicas;
    }

    public void setMinReplicas(int minReplicas) {
        this.minReplicas = minReplicas;
    }

    public int getMaxReplicas() {
        return maxReplicas;
    }

    public void setMaxReplicas(int maxReplicas) {
        this.maxReplicas = maxReplicas;
    }

    public int getTargetCPU() {
        return targetCPU;
    }

    public void setTargetCPU(int targetCPU) {
        this.targetCPU = targetCPU;
    }

    public int getTargetMemory() {
        return targetMemory;
    }

    public void setTargetMemory(int targetMemory) {
        this.targetMemory = targetMemory;
    }
}
