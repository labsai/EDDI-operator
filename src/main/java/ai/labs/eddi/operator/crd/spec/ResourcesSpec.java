package ai.labs.eddi.operator.crd.spec;

/**
 * Kubernetes resource requests and limits specification.
 * Reused across EDDI, database, messaging, and auth components.
 */
public class ResourcesSpec {

    private static final String DEFAULT_REQUEST_CPU = "250m";
    private static final String DEFAULT_REQUEST_MEMORY = "384Mi";
    private static final String DEFAULT_LIMIT_CPU = "2";
    private static final String DEFAULT_LIMIT_MEMORY = "1Gi";

    private ResourceQuantity requests;
    private ResourceQuantity limits;

    public ResourcesSpec() {
        this(DEFAULT_REQUEST_CPU, DEFAULT_REQUEST_MEMORY,
             DEFAULT_LIMIT_CPU, DEFAULT_LIMIT_MEMORY);
    }

    public ResourcesSpec(String requestCpu, String requestMemory,
                          String limitCpu, String limitMemory) {
        this.requests = new ResourceQuantity(requestCpu, requestMemory);
        this.limits = new ResourceQuantity(limitCpu, limitMemory);
    }

    public ResourceQuantity getRequests() {
        return requests;
    }

    public void setRequests(ResourceQuantity requests) {
        this.requests = requests;
    }

    public ResourceQuantity getLimits() {
        return limits;
    }

    public void setLimits(ResourceQuantity limits) {
        this.limits = limits;
    }

    public static class ResourceQuantity {
        private String cpu;
        private String memory;

        public ResourceQuantity() {
            this(DEFAULT_REQUEST_CPU, DEFAULT_REQUEST_MEMORY);
        }

        public ResourceQuantity(String cpu, String memory) {
            this.cpu = cpu;
            this.memory = memory;
        }

        public String getCpu() {
            return cpu;
        }

        public void setCpu(String cpu) {
            this.cpu = cpu;
        }

        public String getMemory() {
            return memory;
        }

        public void setMemory(String memory) {
            this.memory = memory;
        }
    }
}
