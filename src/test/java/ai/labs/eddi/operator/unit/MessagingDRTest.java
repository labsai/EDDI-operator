package ai.labs.eddi.operator.unit;

import ai.labs.eddi.operator.crd.EddiResource;
import ai.labs.eddi.operator.crd.spec.MessagingType;
import ai.labs.eddi.operator.dependent.messaging.NatsServiceDR;
import ai.labs.eddi.operator.dependent.messaging.NatsStatefulSetDR;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static ai.labs.eddi.operator.unit.TestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for messaging dependent resources: NatsStatefulSetDR, NatsServiceDR.
 */
class MessagingDRTest {

    private EddiResource eddi;

    @BeforeEach
    void setUp() {
        eddi = createEddi();
    }

    // ────────────────────────────────────────────────────
    //  NatsStatefulSetDR
    // ────────────────────────────────────────────────────

    @Nested
    class NatsStatefulSet {

        @BeforeEach
        void enableManagedNats() {
            eddi.getSpec().getMessaging().setType(MessagingType.NATS);
            eddi.getSpec().getMessaging().getManaged().setEnabled(true);
        }

        @Test
        void shouldHaveCorrectNameAndNamespace() {
            var sts = callDesired(new NatsStatefulSetDR(), eddi);
            assertThat(sts.getMetadata().getName()).isEqualTo(CR_NAME + "-nats");
            assertThat(sts.getMetadata().getNamespace()).isEqualTo(NAMESPACE);
        }

        @Test
        void shouldEnableJetStreamViaArgs() {
            var sts = callDesired(new NatsStatefulSetDR(), eddi);
            var args = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getArgs();
            assertThat(args).contains("--jetstream", "--store_dir=/data");
        }

        @Test
        void shouldExposePorts4222And8222() {
            var sts = callDesired(new NatsStatefulSetDR(), eddi);
            var ports = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getPorts();
            assertThat(ports).hasSize(2);
            assertThat(ports).anyMatch(p -> p.getContainerPort() == 4222 && "client".equals(p.getName()));
            assertThat(ports).anyMatch(p -> p.getContainerPort() == 8222 && "monitor".equals(p.getName()));
        }

        @Test
        void shouldHaveHttpHealthProbeOnPort8222() {
            var sts = callDesired(new NatsStatefulSetDR(), eddi);
            var container = sts.getSpec().getTemplate().getSpec().getContainers().get(0);
            assertThat(container.getReadinessProbe().getHttpGet().getPath()).isEqualTo("/healthz");
            assertThat(container.getReadinessProbe().getHttpGet().getPort().getIntVal()).isEqualTo(8222);
            assertThat(container.getLivenessProbe().getHttpGet().getPath()).isEqualTo("/healthz");
        }

        @Test
        void shouldMountDataVolume() {
            var sts = callDesired(new NatsStatefulSetDR(), eddi);
            var mounts = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts();
            assertThat(mounts).anyMatch(m -> "data".equals(m.getName()) && "/data".equals(m.getMountPath()));
        }

        @Test
        void shouldHaveVolumeClaimTemplate() {
            var sts = callDesired(new NatsStatefulSetDR(), eddi);
            assertThat(sts.getSpec().getVolumeClaimTemplates()).hasSize(1);
        }

        @Test
        void shouldHaveRestrictedSecurityContext() {
            var sts = callDesired(new NatsStatefulSetDR(), eddi);
            var sc = sts.getSpec().getTemplate().getSpec().getContainers().get(0).getSecurityContext();
            assertThat(sc.getRunAsNonRoot()).isTrue();
            assertThat(sc.getAllowPrivilegeEscalation()).isFalse();
        }

        @Test
        void shouldHaveStandardLabels() {
            var sts = callDesired(new NatsStatefulSetDR(), eddi);
            assertThat(sts.getMetadata().getLabels())
                    .containsEntry("app.kubernetes.io/component", "nats")
                    .containsEntry("app.kubernetes.io/managed-by", "eddi-operator");
        }
    }

    // ────────────────────────────────────────────────────
    //  NatsServiceDR
    // ────────────────────────────────────────────────────

    @Nested
    class NatsService {

        @Test
        void shouldBeHeadlessServiceWithTwoPorts() {
            var svc = callDesired(new NatsServiceDR(), eddi);
            assertThat(svc.getMetadata().getName()).isEqualTo(CR_NAME + "-nats");
            assertThat(svc.getSpec().getClusterIP()).isEqualTo("None");
            assertThat(svc.getSpec().getPorts()).hasSize(2);
            assertThat(svc.getSpec().getPorts()).anyMatch(p -> p.getPort() == 4222);
            assertThat(svc.getSpec().getPorts()).anyMatch(p -> p.getPort() == 8222);
        }

        @Test
        void shouldSelectNatsComponent() {
            var svc = callDesired(new NatsServiceDR(), eddi);
            assertThat(svc.getSpec().getSelector())
                    .containsEntry("app.kubernetes.io/component", "nats");
        }
    }
}
