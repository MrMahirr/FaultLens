package com.faultlens.collector.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.faultlens.collector.config.CollectorProperties;
import com.faultlens.collector.model.LogSource;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.LogSourceType;
import io.fabric8.kubernetes.api.model.NamespaceListBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.PodListBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link KubernetesAdapter} against fabric8's mock API server.
 *
 * <p>Documents the <b>current</b> polling behaviour before P06 changes it: every poll re-reads
 * each pod's full log, so lines already sent are emitted again (see
 * {@link #currentBehaviour_eachPollReEmitsTheWholeLog()}).
 */
@EnableKubernetesMockClient
class KubernetesAdapterTest {
    private static final String NAMESPACE = "team";
    private static final Duration WAIT = Duration.ofSeconds(15);

    KubernetesMockServer server;
    KubernetesClient client;

    @TempDir
    Path tmp;

    private KubernetesAdapter adapter;
    private final List<LogEventDto> events = new CopyOnWriteArrayList<>();
    private Thread streamer;

    @BeforeEach
    void setUp() {
        CollectorProperties properties = new CollectorProperties();
        properties.getK8s().setReconnectIntervalSeconds(1);
        adapter = new KubernetesAdapter(properties);
    }

    @AfterEach
    void tearDown() throws Exception {
        adapter.stopStreaming("7");
        if (streamer != null) {
            streamer.interrupt();
            streamer.join(TimeUnit.SECONDS.toMillis(5));
        }
    }

    @Test
    void emitsOneEventPerNonBlankLogLineWithPodMetadata() throws Exception {
        expectPods(pod("orders-1", "orders", "node-a"));
        expectLog("orders-1", "started\n\nNullPointerException at Foo\n");
        start(source());

        await().atMost(WAIT).until(() -> events.size() >= 2);
        assertThat(events.subList(0, 2)).extracting(LogEventDto::getMessage)
                .containsExactly("started", "NullPointerException at Foo");

        LogEventDto event = events.getFirst();
        assertThat(event.getSource()).isEqualTo("kubernetes");
        assertThat(event.getCluster()).isEqualTo("prod-eu");
        assertThat(event.getNamespace()).isEqualTo(NAMESPACE);
        assertThat(event.getPodName()).isEqualTo("orders-1");
        assertThat(event.getNodeName()).isEqualTo("node-a");
        assertThat(event.getContainerName()).isEqualTo("app");
        assertThat(event.getServiceName()).as("taken from the 'app' label").isEqualTo("orders");
        assertThat(event.getLabels()).containsEntry("sourceId", "7").containsEntry("app", "orders");
    }

    @Test
    void readsEveryPodInTheNamespace() throws Exception {
        expectPods(pod("orders-1", "orders", "node-a"), pod("billing-1", null, "node-b"));
        expectLog("orders-1", "from orders\n");
        expectLog("billing-1", "from billing\n");
        start(source());

        await().atMost(WAIT).until(() -> events.stream().map(LogEventDto::getPodName).distinct().count() == 2);
        LogEventDto billing = events.stream().filter(e -> "billing-1".equals(e.getPodName())).findFirst().orElseThrow();
        assertThat(billing.getMessage()).isEqualTo("from billing");
        assertThat(billing.getServiceName()).as("no 'app' label → source name").isEqualTo("k8s-source");
    }

    @Test
    void currentBehaviour_eachPollReEmitsTheWholeLog() throws Exception {
        expectPods(pod("orders-1", "orders", "node-a"));
        expectLog("orders-1", "only line\n");
        start(source());

        // Poll interval is 1s; after two polls the single line has been sent twice.
        await().atMost(WAIT).until(() -> events.size() >= 2);
        assertThat(events.subList(0, 2)).extracting(LogEventDto::getMessage).containsOnly("only line");
    }

    @Test
    void testConnectionSucceedsWhenTheApiAnswers() throws Exception {
        server.expect().get().withPath("/api/v1/namespaces")
                .andReturn(200, new NamespaceListBuilder().build()).always();

        assertThat(adapter.testConnection(source())).isTrue();
    }

    @Test
    void testConnectionFailsWhenAccessIsDenied() throws Exception {
        // 403 rather than 5xx: fabric8 retries 5xx with backoff, which only slows the test down.
        server.expect().get().withPath("/api/v1/namespaces").andReturn(403, "forbidden").always();

        assertThat(adapter.testConnection(source())).isFalse();
    }

    private void expectPods(Pod... pods) {
        server.expect().get().withPath("/api/v1/namespaces/" + NAMESPACE + "/pods")
                .andReturn(200, new PodListBuilder().withItems(pods).build()).always();
    }

    private void expectLog(String podName, String body) {
        server.expect().get().withPath("/api/v1/namespaces/" + NAMESPACE + "/pods/" + podName + "/log?pretty=false")
                .andReturn(200, body).always();
    }

    private static Pod pod(String name, String appLabel, String node) {
        PodBuilder builder = new PodBuilder().withNewMetadata().withName(name).withNamespace(NAMESPACE).endMetadata()
                .withNewSpec().withNodeName(node).endSpec();
        if (appLabel != null) {
            builder.editMetadata().addToLabels("app", appLabel).endMetadata();
        }
        return builder.build();
    }

    /** Source whose kubeconfig points at the mock server. */
    private LogSource source() throws Exception {
        String kubeconfig = """
                apiVersion: v1
                kind: Config
                clusters:
                - name: mock
                  cluster:
                    server: %s
                    insecure-skip-tls-verify: true
                contexts:
                - name: mock
                  context:
                    cluster: mock
                    user: mock
                    namespace: %s
                current-context: mock
                users:
                - name: mock
                  user:
                    token: test-token
                """.formatted(client.getConfiguration().getMasterUrl(), NAMESPACE);
        Path file = Files.writeString(tmp.resolve("kubeconfig"), kubeconfig);
        String json = """
                {"namespace":"%s","cluster":"prod-eu","containerName":"app","kubeconfigPath":"%s"}"""
                .formatted(NAMESPACE, file.toString().replace("\\", "\\\\"));
        return LogSource.builder().id(7L).name("k8s-source").type(LogSourceType.KUBERNETES).config(json).build();
    }

    private void start(LogSource source) {
        streamer = new Thread(() -> {
            try {
                adapter.startStreaming(source, events::add);
            } catch (RuntimeException ignored) {
                // surfaced through the assertions instead
            }
        }, "k8s-test");
        streamer.setDaemon(true);
        streamer.start();
    }
}
