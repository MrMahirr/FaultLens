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
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * {@link KubernetesAdapter} against fabric8's mock API server. Logs are requested with
 * {@code timestamps=true&sinceTime=...}; only lines newer than the last emitted one are sent on.
 */
@EnableKubernetesMockClient
class KubernetesAdapterTest {
    private static final String NAMESPACE = "team";
    private static final Duration WAIT = Duration.ofSeconds(15);
    private static final String T1 = "2026-11-05T10:00:01.500Z";
    private static final String T2 = "2026-11-05T10:00:02.250Z";

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
    void emitsOneEventPerNonBlankLogLineWithPodMetadataAndLogTimestamp() throws Exception {
        expectPods(pod("orders-1", "orders", "node-a"));
        expectLog("orders-1", Instant.EPOCH.toString(), T1 + " started\n\n" + T2 + " NullPointerException at Foo\n");
        start(source(null));

        await().atMost(WAIT).until(() -> events.size() >= 2);
        assertThat(events).extracting(LogEventDto::getMessage)
                .containsExactly("started", "NullPointerException at Foo");

        LogEventDto event = events.getFirst();
        assertThat(event.getTimestamp()).as("taken from the log line").isEqualTo(Instant.parse(T1));
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
        expectLog("orders-1", Instant.EPOCH.toString(), T1 + " from orders\n");
        expectLog("billing-1", Instant.EPOCH.toString(), T1 + " from billing\n");
        start(source(null));

        await().atMost(WAIT).until(() -> events.stream().map(LogEventDto::getPodName).distinct().count() == 2);
        LogEventDto billing = events.stream().filter(e -> "billing-1".equals(e.getPodName())).findFirst().orElseThrow();
        assertThat(billing.getMessage()).isEqualTo("from billing");
        assertThat(billing.getServiceName()).as("no 'app' label → source name").isEqualTo("k8s-source");
    }

    @Test
    void laterPollsSendOnlyNewLines() throws Exception {
        // Regression (P06): every poll used to re-send the pod's whole log.
        expectPods(pod("orders-1", "orders", "node-a"));
        expectLog("orders-1", Instant.EPOCH.toString(), T1 + " first\n");
        // The next request asks since T1; the API repeats the boundary line (second precision).
        expectLog("orders-1", T1, T1 + " first\n" + T2 + " second\n");
        expectLog("orders-1", T2, T2 + " second\n");
        start(source(null));

        await().atMost(WAIT).until(() -> events.size() >= 2);
        Thread.sleep(2500); // at least two more polls
        assertThat(events).extracting(LogEventDto::getMessage).containsExactly("first", "second");
    }

    @Test
    void knownSourceResumesFromLastSeenAtInsteadOfReplaying() throws Exception {
        Instant lastSeen = Instant.parse("2026-11-05T09:59:00Z");
        expectPods(pod("orders-1", "orders", "node-a"));
        expectLog("orders-1", lastSeen.toString(), "2026-11-05T09:58:59Z old\n" + T1 + " new\n");
        expectLog("orders-1", T1, T1 + " new\n");
        start(source(lastSeen));

        await().atMost(WAIT).until(() -> !events.isEmpty());
        Thread.sleep(1500);
        assertThat(events).extracting(LogEventDto::getMessage).containsExactly("new");
    }

    @Test
    void newLinesFiltersByTimestampAndKeepsLinesWithoutOne() {
        Instant since = Instant.parse(T1);
        String log = T1 + " boundary\n" + T2 + " after\n\nplain line without timestamp\n" + T2 + "   \n";

        assertThat(KubernetesAdapter.newLines(log, since))
                .extracting(KubernetesAdapter.TimedLine::text)
                .containsExactly("after", "plain line without timestamp");
        assertThat(KubernetesAdapter.newLines(log, null)).hasSize(3);
        assertThat(KubernetesAdapter.newLines(null, since)).isEmpty();
    }

    @Test
    void testConnectionSucceedsWhenTheApiAnswers() throws Exception {
        server.expect().get().withPath("/api/v1/namespaces")
                .andReturn(200, new NamespaceListBuilder().build()).always();

        assertThat(adapter.testConnection(source(null))).isTrue();
    }

    @Test
    void testConnectionFailsWhenAccessIsDenied() throws Exception {
        // 403 rather than 5xx: fabric8 retries 5xx with backoff, which only slows the test down.
        server.expect().get().withPath("/api/v1/namespaces").andReturn(403, "forbidden").always();

        assertThat(adapter.testConnection(source(null))).isFalse();
    }

    private void expectPods(Pod... pods) {
        server.expect().get().withPath("/api/v1/namespaces/" + NAMESPACE + "/pods")
                .andReturn(200, new PodListBuilder().withItems(pods).build()).always();
    }

    private void expectLog(String podName, String sinceTime, String body) {
        server.expect().get()
                .withPath("/api/v1/namespaces/" + NAMESPACE + "/pods/" + podName
                        + "/log?pretty=false&sinceTime=" + sinceTime + "&timestamps=true")
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
    private LogSource source(Instant lastSeenAt) throws Exception {
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
        return LogSource.builder().id(7L).name("k8s-source").type(LogSourceType.KUBERNETES).config(json)
                .lastSeenAt(lastSeenAt).build();
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
