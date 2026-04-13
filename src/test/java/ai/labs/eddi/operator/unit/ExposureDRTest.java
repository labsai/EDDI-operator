package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.ExposureType;
import ai.labs.eddi.operator.dependent.exposure.IngressDR;
import ai.labs.eddi.operator.dependent.exposure.RouteDR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static ai.labs.eddi.operator.unit.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for exposure dependent resources: IngressDR, RouteDR.
 */
class ExposureDRTest {

    private EddiResource eddi;

    @BeforeEach
    void setUp() {
        eddi = createEddi();
        eddi.getSpec().getExposure().setHost("eddi.example.com");
    }

    // ────────────────────────────────────────────────────
    //  IngressDR
    // ────────────────────────────────────────────────────

    @Nested
    class Ingress {

        @Test
        void shouldHaveCorrectNameAndNamespace() {
            var ingress = callDesired(new IngressDR(), eddi);
            assertThat(ingress.getMetadata().getName()).isEqualTo(CR_NAME + "-ingress");
            assertThat(ingress.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
        }

        @Test
        void shouldHaveRuleWithHost() {
            var ingress = callDesired(new IngressDR(), eddi);
            var rules = ingress.getSpec().getRules();
            assertThat(rules).hasSize(1);
            assertThat(rules.get(0).getHost()).isEqualTo("eddi.example.com");
        }

        @Test
        void shouldRouteToServerServiceOnPort8080() {
            var ingress = callDesired(new IngressDR(), eddi);
            var path = ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0);
            assertThat(path.getPath()).isEqualTo("/");
            assertThat(path.getPathType()).isEqualTo("Prefix");
            assertThat(path.getBackend().getService().getName()).isEqualTo(CR_NAME + "-server");
            assertThat(path.getBackend().getService().getPort().getNumber()).isEqualTo(8080);
        }

        @Test
        void shouldHaveTlsByDefault() {
            // TLS is enabled by default in EddiSpec
            var ingress = callDesired(new IngressDR(), eddi);
            assertThat(ingress.getSpec().getTls()).hasSize(1);
            assertThat(ingress.getSpec().getTls().get(0).getHosts()).contains("eddi.example.com");
        }

        @Test
        void shouldNotHaveTlsWhenDisabled() {
            eddi.getSpec().getExposure().getTls().setEnabled(false);

            var ingress = callDesired(new IngressDR(), eddi);
            assertThat(ingress.getSpec().getTls()).isNullOrEmpty();
        }

        @Test
        void shouldSetIngressClassName() {
            eddi.getSpec().getExposure().setIngressClassName("nginx");

            var ingress = callDesired(new IngressDR(), eddi);
            assertThat(ingress.getSpec().getIngressClassName()).isEqualTo("nginx");
        }

        @Test
        void shouldHaveStandardLabels() {
            var ingress = callDesired(new IngressDR(), eddi);
            assertThat(ingress.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/component", "ingress")
                    .containsEntry("app.kubernetes.io/managed-by", "eddi-operator");
        }
    }

    // ────────────────────────────────────────────────────
    //  RouteDR
    // ────────────────────────────────────────────────────

    @Nested
    class Route {

        @Test
        void shouldHaveCorrectNameAndNamespace() {
            var route = callDesired(new RouteDR(), eddi);
            assertThat(route.getMetadata().getName()).isEqualTo(CR_NAME + "-route");
            assertThat(route.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
        }

        @Test
        void shouldTargetServerServiceWithWeight100() {
            var route = callDesired(new RouteDR(), eddi);
            assertThat(route.getSpec().getTo().getKind()).isEqualTo("Service");
            assertThat(route.getSpec().getTo().getName()).isEqualTo(CR_NAME + "-server");
            assertThat(route.getSpec().getTo().getWeight()).isEqualTo(100);
        }

        @Test
        void shouldTargetHttpPort() {
            var route = callDesired(new RouteDR(), eddi);
            assertThat(route.getSpec().getPort().getTargetPort().getStrVal()).isEqualTo("http");
        }

        @Test
        void shouldSetHostWhenConfigured() {
            var route = callDesired(new RouteDR(), eddi);
            assertThat(route.getSpec().getHost()).isEqualTo("eddi.example.com");
        }

        @Test
        void shouldOmitHostWhenBlank() {
            eddi.getSpec().getExposure().setHost("");
            var route = callDesired(new RouteDR(), eddi);
            assertThat(route.getSpec().getHost()).isNullOrEmpty();
        }

        @Test
        void shouldHaveEdgeTlsByDefault() {
            // TLS is enabled by default in EddiSpec
            var route = callDesired(new RouteDR(), eddi);
            assertThat(route.getSpec().getTls()).isNotNull();
            assertThat(route.getSpec().getTls().getTermination()).isEqualTo("edge");
            assertThat(route.getSpec().getTls().getInsecureEdgeTerminationPolicy()).isEqualTo("Redirect");
        }

        @Test
        void shouldNotHaveTlsWhenDisabled() {
            eddi.getSpec().getExposure().getTls().setEnabled(false);

            var route = callDesired(new RouteDR(), eddi);
            assertThat(route.getSpec().getTls()).isNull();
        }

        @Test
        void shouldHaveStandardLabels() {
            var route = callDesired(new RouteDR(), eddi);
            assertThat(route.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/component", "route")
                    .containsEntry("app.kubernetes.io/managed-by", "eddi-operator");
        }
    }
}
