package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.KeycloakMode;
import ai.labs.eddi.operator.dependent.auth.KeycloakDeploymentDR;
import ai.labs.eddi.operator.dependent.auth.KeycloakSecretDR;
import ai.labs.eddi.operator.dependent.auth.KeycloakServiceDR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static ai.labs.eddi.operator.unit.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for auth dependent resources: KeycloakDeploymentDR, KeycloakServiceDR.
 * <p>
 * KeycloakSecretDR.desired() reads from Context to preserve existing credentials —
 * only the static helper is tested here, the full DR is tested via integration tests.
 * </p>
 */
class AuthDRTest {

    private EddiResource eddi;

    @BeforeEach
    void setUp() {
        eddi = createEddi();
        eddi.getSpec().getAuth().setEnabled(true);
        eddi.getSpec().getAuth().getManaged().setEnabled(true);
    }

    // ────────────────────────────────────────────────────
    //  KeycloakDeploymentDR
    // ────────────────────────────────────────────────────

    @Nested
    class KeycloakDeployment {

        @Test
        void shouldHaveCorrectNameAndNamespace() {
            var deploy = callDesired(new KeycloakDeploymentDR(), eddi);
            assertThat(deploy.getMetadata().getName()).isEqualTo(CR_NAME + "-keycloak");
            assertThat(deploy.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
        }

        @Test
        void shouldStartInDevModeByDefault() {
            var deploy = callDesired(new KeycloakDeploymentDR(), eddi);
            var args = deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getArgs();
            assertThat(args).contains("start-dev");
        }

        @Test
        void shouldStartInProductionMode() {
            eddi.getSpec().getAuth().getManaged().setMode(KeycloakMode.PRODUCTION);

            var deploy = callDesired(new KeycloakDeploymentDR(), eddi);
            var args = deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getArgs();
            assertThat(args).contains("start");
            assertThat(args).doesNotContain("start-dev");
        }

        @Test
        void shouldReferenceAdminCredentialSecret() {
            var deploy = callDesired(new KeycloakDeploymentDR(), eddi);
            var envVars = deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();

            var adminUser = envVars.stream()
                    .filter(e -> "KEYCLOAK_ADMIN".equals(e.getName()))
                    .findFirst().orElseThrow();
            assertThat(adminUser.getValueFrom().getSecretKeyRef().getName())
                    .isEqualTo(CR_NAME + "-keycloak-admin");
            assertThat(adminUser.getValueFrom().getSecretKeyRef().getKey()).isEqualTo("username");

            var adminPwd = envVars.stream()
                    .filter(e -> "KEYCLOAK_ADMIN_PASSWORD".equals(e.getName()))
                    .findFirst().orElseThrow();
            assertThat(adminPwd.getValueFrom().getSecretKeyRef().getKey()).isEqualTo("password");
        }

        @Test
        void shouldUseExternalSecretRefWhenConfigured() {
            eddi.getSpec().getAuth().getManaged().setAdminSecretRef("my-keycloak-creds");

            var deploy = callDesired(new KeycloakDeploymentDR(), eddi);
            var envVars = deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
            var adminUser = envVars.stream()
                    .filter(e -> "KEYCLOAK_ADMIN".equals(e.getName()))
                    .findFirst().orElseThrow();
            assertThat(adminUser.getValueFrom().getSecretKeyRef().getName())
                    .isEqualTo("my-keycloak-creds");
        }

        @Test
        void shouldExposePort8080() {
            var deploy = callDesired(new KeycloakDeploymentDR(), eddi);
            var ports = deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getPorts();
            assertThat(ports).hasSize(1);
            assertThat(ports.get(0).getContainerPort()).isEqualTo(8080);
        }

        @Test
        void shouldHaveHealthProbes() {
            var deploy = callDesired(new KeycloakDeploymentDR(), eddi);
            var container = deploy.getSpec().getTemplate().getSpec().getContainers().get(0);
            assertThat(container.getReadinessProbe().getHttpGet().getPath()).isEqualTo("/health/ready");
            assertThat(container.getLivenessProbe().getHttpGet().getPath()).isEqualTo("/health/live");
        }

        @Test
        void shouldHaveRestrictedSecurityContext() {
            var deploy = callDesired(new KeycloakDeploymentDR(), eddi);
            var sc = deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getSecurityContext();
            assertThat(sc.getRunAsNonRoot()).isTrue();
            assertThat(sc.getAllowPrivilegeEscalation()).isFalse();
        }

        @Test
        void shouldHaveStandardLabels() {
            var deploy = callDesired(new KeycloakDeploymentDR(), eddi);
            assertThat(deploy.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/component", "keycloak");
        }
    }

    // ────────────────────────────────────────────────────
    //  KeycloakServiceDR
    // ────────────────────────────────────────────────────

    @Nested
    class KeycloakService {

        @Test
        void shouldBeClusterIPServiceOnPort8080() {
            var svc = callDesired(new KeycloakServiceDR(), eddi);
            assertThat(svc.getMetadata().getName()).isEqualTo(CR_NAME + "-keycloak");
            assertThat(svc.getSpec().getType()).isEqualTo("ClusterIP");
            assertThat(svc.getSpec().getPorts()).hasSize(1);
            assertThat(svc.getSpec().getPorts().get(0).getPort()).isEqualTo(8080);
        }

        @Test
        void shouldSelectKeycloakComponent() {
            var svc = callDesired(new KeycloakServiceDR(), eddi);
            assertThat(svc.getSpec().getSelector())
                    .containsEntry("app.kubernetes.io/component", "keycloak");
        }
    }

    // ────────────────────────────────────────────────────
    //  KeycloakSecretDR — static helper only
    // ────────────────────────────────────────────────────

    @Nested
    class KeycloakSecretGeneration {

        @Test
        void shouldGeneratePasswordOfCorrectLength() {
            var password = KeycloakSecretDR.generatePassword();
            assertThat(password).hasSize(24);
        }

        @Test
        void shouldGenerateUniquePasswords() {
            var p1 = KeycloakSecretDR.generatePassword();
            var p2 = KeycloakSecretDR.generatePassword();
            assertThat(p1).isNotEqualTo(p2);
        }
    }
}
