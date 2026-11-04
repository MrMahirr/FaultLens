package com.faultlens.collector.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.faultlens.collector.config.CollectorProperties;
import com.faultlens.collector.model.LogSource;
import com.faultlens.common.dto.LogEventDto;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.enums.Severity;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tailing behaviour of {@link LocalFileAdapter}; the allow-list rules themselves are covered by
 * {@link LocalFilePathPolicyTest}, here only that the adapter enforces them before reading.
 */
class LocalFileAdapterTest {
    private static final Duration WAIT = Duration.ofSeconds(10);

    @TempDir
    Path root;

    private Path allowedDir;
    private LocalFileAdapter adapter;
    private final List<LogEventDto> events = new CopyOnWriteArrayList<>();
    private Thread streamer;

    @BeforeEach
    void setUp() throws Exception {
        allowedDir = Files.createDirectories(root.resolve("logs"));
        CollectorProperties properties = new CollectorProperties();
        properties.getFile().setAllowedDirs(List.of(allowedDir.toString()));
        adapter = new LocalFileAdapter(new LocalFilePathPolicy(properties));
    }

    @AfterEach
    void tearDown() throws Exception {
        // No interrupt: the adapter treats it as a read error and backs off 5s. Stopping is enough,
        // the loop exits after its current 1s sleep.
        adapter.stopStreaming("1");
        if (streamer != null) {
            streamer.join(TimeUnit.SECONDS.toMillis(5));
        }
    }

    @Test
    void emitsOnlyLinesAppendedAfterStart() throws Exception {
        Path file = Files.writeString(allowedDir.resolve("app.log"), "history line\n");
        start(source(file));

        append(file, "first new line\nsecond new line\n");

        await().atMost(WAIT).until(() -> events.size() >= 2);
        assertThat(events).extracting(LogEventDto::getMessage).containsExactly("first new line", "second new line");
        LogEventDto event = events.getFirst();
        assertThat(event.getSource()).isEqualTo("local_file");
        assertThat(event.getServiceName()).isEqualTo("app-file");
        assertThat(event.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(event.getRawLine()).isEqualTo("first new line");
        assertThat(event.getLabels()).containsEntry("sourceId", "1");
        assertThat(event.getId()).isNotBlank();
        assertThat(event.getTimestamp()).isNotNull();
    }

    @Test
    void decodesUtf8Lines() throws Exception {
        Path file = Files.writeString(allowedDir.resolve("app.log"), "");
        start(source(file));

        append(file, "Bağlantı hatası: çözülemedi\n");

        await().atMost(WAIT).until(() -> !events.isEmpty());
        assertThat(events.getFirst().getMessage()).isEqualTo("Bağlantı hatası: çözülemedi");
    }

    @Test
    void restartsFromTheTopAfterTruncation() throws Exception {
        Path file = Files.writeString(allowedDir.resolve("app.log"), "a fairly long history line\n");
        start(source(file));

        Files.writeString(file, "rotated\n", StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);

        await().atMost(WAIT).until(() -> !events.isEmpty());
        assertThat(events).extracting(LogEventDto::getMessage).containsExactly("rotated");
    }

    @Test
    void picksUpAFileCreatedAfterStart() throws Exception {
        Path file = allowedDir.resolve("later.log");
        start(source(file));

        Files.writeString(file, "created later\n");

        await().atMost(WAIT).until(() -> !events.isEmpty());
        assertThat(events.getFirst().getMessage()).isEqualTo("created later");
    }

    @Test
    void stopsWhenRequested() throws Exception {
        Path file = Files.writeString(allowedDir.resolve("app.log"), "");
        start(source(file));

        adapter.stopStreaming("1");

        streamer.join(TimeUnit.SECONDS.toMillis(5));
        assertThat(streamer.isAlive()).isFalse();
    }

    @Test
    void refusesPathOutsideAllowedDirsWithoutReading() throws Exception {
        Path outside = Files.writeString(Files.createDirectories(root.resolve("secrets")).resolve(".env"), "DB_PASS=x\n");

        // Returns immediately instead of entering the tail loop.
        adapter.startStreaming(source(outside), events::add);

        assertThat(events).isEmpty();
    }

    @Test
    void refusesMissingPathWithoutReading() {
        LogSource source = LogSource.builder().id(1L).name("f").type(LogSourceType.LOCAL_FILE).config("{}").build();

        adapter.startStreaming(source, events::add);

        assertThat(events).isEmpty();
    }

    @Test
    void testConnectionChecksAllowListAndParentDirectory() throws Exception {
        assertThat(adapter.testConnection(source(Files.writeString(allowedDir.resolve("app.log"), "")))).isTrue();
        assertThat(adapter.testConnection(source(allowedDir.resolve("not-yet.log")))).isTrue();
        assertThat(adapter.testConnection(source(allowedDir.resolve("missing-dir/app.log")))).isFalse();
        assertThat(adapter.testConnection(source(root.resolve("outside.log")))).isFalse();
    }

    private void start(LogSource source) throws InterruptedException {
        streamer = new Thread(() -> adapter.startStreaming(source, events::add), "local-file-test");
        streamer.setDaemon(true);
        streamer.start();
        // The adapter records the start offset right away; give it a moment before writing.
        Thread.sleep(300);
    }

    private static void append(Path file, String text) throws Exception {
        Files.writeString(file, text, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
    }

    private static LogSource source(Path path) {
        String json = "{\"logFilePath\":\"" + path.toString().replace("\\", "\\\\") + "\"}";
        return LogSource.builder().id(1L).name("app-file").type(LogSourceType.LOCAL_FILE).config(json).build();
    }
}
