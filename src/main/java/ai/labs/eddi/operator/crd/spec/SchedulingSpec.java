package ai.labs.eddi.operator.crd.spec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.fabric8.kubernetes.api.model.Affinity;
import io.fabric8.kubernetes.api.model.Toleration;
import io.fabric8.kubernetes.api.model.TopologySpreadConstraint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pod scheduling constraints for enterprise Kubernetes clusters.
 * Supports nodeSelector, tolerations, affinity, and topologySpreadConstraints
 * for workload isolation, zone-awareness, and node targeting.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchedulingSpec {

    /**
     * NodeSelector for simple label-based node targeting.
     * Example: {@code kubernetes.io/arch: amd64}
     */
    private Map<String, String> nodeSelector = new LinkedHashMap<>();

    /**
     * Tolerations to schedule onto tainted nodes.
     */
    private List<Toleration> tolerations = new ArrayList<>();

    /**
     * Affinity rules for advanced pod placement (node affinity, pod affinity/anti-affinity).
     */
    private Affinity affinity;

    /**
     * Topology spread constraints for zone/node distribution.
     */
    private List<TopologySpreadConstraint> topologySpreadConstraints = new ArrayList<>();

    public Map<String, String> getNodeSelector() {
        return nodeSelector;
    }

    public void setNodeSelector(Map<String, String> nodeSelector) {
        this.nodeSelector = nodeSelector;
    }

    public List<Toleration> getTolerations() {
        return tolerations;
    }

    public void setTolerations(List<Toleration> tolerations) {
        this.tolerations = tolerations;
    }

    public Affinity getAffinity() {
        return affinity;
    }

    public void setAffinity(Affinity affinity) {
        this.affinity = affinity;
    }

    public List<TopologySpreadConstraint> getTopologySpreadConstraints() {
        return topologySpreadConstraints;
    }

    public void setTopologySpreadConstraints(List<TopologySpreadConstraint> topologySpreadConstraints) {
        this.topologySpreadConstraints = topologySpreadConstraints;
    }
}
