package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.dependent.extras.ManagerDeploymentDR;
import ai.labs.eddi.operator.dependent.extras.ManagerServiceDR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static ai.labs.eddi.operator.unit.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for Manager UI dependent resources:
 * ManagerDeploymentDR, ManagerServiceDR.
 */
class ManagerDRTest {

    private EddiResource eddi;

    @BeforeEach
    void setUp() {
        eddi = createEddi();
        eddi.getSpec().getManager().setEnabled(true);
    }

    // ────────────────────────────────────────────────────
    //  ManagerDeploymentDR
    // ────────────────────────────────────────────────────

    @Nested
    class ManagerDeployment {

        @Test
        void shouldHaveCorrectNameAndNamespace() {
            var deploy = callDesired(new ManagerDeploymentDR(), eddi);
            assertThat(deploy.getMetadata().getName()).isEqualTo(CR_NAME + "-manager");
            assertThat(deploy.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
        }

        @Test
        void shouldExposePort3000() {
            var deploy = callDesired(new ManagerDeploymentDR(), eddi);
            var ports = deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getPorts();
            assertThat(ports).hasSize(1);
            assertThat(ports.get(0).getContainerPort()).isEqualTo(3000);
        }

        @Test
        void shouldSetEddiApiUrlEnvVar() {
            var deploy = callDesired(new ManagerDeploymentDR(), eddi);
            var envVars = deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
            var apiUrl = envVars.stream()
                    .filter(e -> "EDDI_API_URL".equals(e.getName()))
                    .findFirst().orElseThrow();
            assertThat(apiUrl.getValue()).isEqualTo("http://" + CR_NAME + "-server:8080");
        }

        @Test
        void shouldHaveReadinessProbe() {
            var deploy = callDesired(new ManagerDeploymentDR(), eddi);
            var probe = deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getReadinessProbe();
            assertThat(probe).isNotNull();
            assertThat(probe.getHttpGet().getPath()).isEqualTo("/");
            assertThat(probe.getHttpGet().getPort().getIntVal()).isEqualTo(3000);
        }

        @Test
        void shouldHaveRestrictedSecurityContext() {
            var deploy = callDesired(new ManagerDeploymentDR(), eddi);
            var sc = deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getSecurityContext();
            assertThat(sc.getRunAsNonRoot()).isTrue();
            assertThat(sc.getAllowPrivilegeEscalation()).isFalse();
        }

        @Test
        void shouldHaveStandardLabels() {
            var deploy = callDesired(new ManagerDeploymentDR(), eddi);
            assertThat(deploy.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/component", "manager")
                    .containsEntry("app.kubernetes.io/managed-by", "eddi-operator");
        }

        @Test
        void shouldHaveOneReplica() {
            var deploy = callDesired(new ManagerDeploymentDR(), eddi);
            assertThat(deploy.getSpec().getReplicas()).isEqualTo(1);
        }
    }

    // ────────────────────────────────────────────────────
    //  ManagerServiceDR
    // ────────────────────────────────────────────────────

    @Nested
    class ManagerService {

        @Test
        void shouldBeClusterIPServiceOnPort3000() {
            var svc = callDesired(new ManagerServiceDR(), eddi);
            assertThat(svc.getMetadata().getName()).isEqualTo(CR_NAME + "-manager");
            assertThat(svc.getSpec().getType()).isEqualTo("ClusterIP");
            assertThat(svc.getSpec().getPorts()).hasSize(1);
            assertThat(svc.getSpec().getPorts().get(0).getPort()).isEqualTo(3000);
        }

        @Test
        void shouldSelectManagerComponent() {
            var svc = callDesired(new ManagerServiceDR(), eddi);
            assertThat(svc.getSpec().getSelector())
                    .containsEntry("app.kubernetes.io/component", "manager");
        }
    }
}
