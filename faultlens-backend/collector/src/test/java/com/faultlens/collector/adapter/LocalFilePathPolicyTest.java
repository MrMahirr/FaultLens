package com.faultlens.collector.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;

import com.faultlens.collector.config.CollectorProperties;
import com.faultlens.collector.model.LogSource;
import com.faultlens.common.enums.LogSourceType;
import com.faultlens.common.exception.CollectorException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFilePathPolicyTest {
    @TempDir
    Path root;

    private Path allowedDir;
    private Path outsideDir;
    private LocalFilePathPolicy policy;

    @BeforeEach
    void setUp() throws Exception {
        allowedDir = Files.createDirectories(root.resolve("logs"));
        outsideDir = Files.createDirectories(root.resolve("secrets"));
        Files.writeString(allowedDir.resolve("app.log"), "line\n");
        Files.writeString(outsideDir.resolve(".env"), "DB_PASS=x\n");
        policy = policyFor(allowedDir.toString());
    }

    @Test
    void acceptsFileInsideAllowedDirectory() {
        assertThat(policy.requireAllowed(allowedDir.resolve("app.log").toString()))
                .isEqualTo(policy.allowedDirs().get(0).resolve("app.log"));
        // not created yet (log rotation) is fine as long as it is inside
        assertThat(policy.isAllowed(allowedDir.resolve("nested/future.log").toString())).isTrue();
    }

    @Test
    void rejectsFileOutsideAllowedDirectory() {
        assertRejected(outsideDir.resolve(".env").toString());
    }

    @Test
    void rejectsTraversalOutOfAllowedDirectory() {
        assertRejected(allowedDir + "/../secrets/.env");
    }

    @Test
    void rejectsSiblingDirectoryWithSamePrefix() throws Exception {
        Path sibling = Files.createDirectories(root.resolve("logs-private"));
        Files.writeString(sibling.resolve("x.log"), "x");
        assertRejected(sibling.resolve("x.log").toString());
    }

    @Test
    void rejectsAllowedDirectoryItselfAndBlankPaths() {
        assertRejected(allowedDir.toString());
        assertRejected("");
        assertRejected(null);
    }

    @Test
    void rejectsSymlinkThatEscapesAllowedDirectory() throws Exception {
        Path link = allowedDir.resolve("escape.log");
        try {
            Files.createSymbolicLink(link, outsideDir.resolve(".env"));
        } catch (Exception | Error ex) {
            assumeThat(false).as("symlinks not permitted on this machine: %s", ex).isTrue();
        }
        assertRejected(link.toString());
    }

    @Test
    void rejectsEverythingWhenNoDirectoryIsAllowed() {
        LocalFilePathPolicy none = policyFor();
        assertThat(none.isAllowed(allowedDir.resolve("app.log").toString())).isFalse();
    }

    @Test
    void adapterConnectionTestFailsForDisallowedPath() {
        LocalFileAdapter adapter = new LocalFileAdapter(policy);

        assertThat(adapter.testConnection(source(outsideDir.resolve(".env")))).isFalse();
        assertThat(adapter.testConnection(source(allowedDir.resolve("app.log")))).isTrue();
    }

    private void assertRejected(String path) {
        assertThatThrownBy(() -> policy.requireAllowed(path))
                .isInstanceOf(CollectorException.class)
                .hasFieldOrPropertyWithValue("errorCode", "LOCAL_FILE_PATH_NOT_ALLOWED");
    }

    private static LocalFilePathPolicy policyFor(String... dirs) {
        CollectorProperties properties = new CollectorProperties();
        properties.getFile().setAllowedDirs(List.of(dirs));
        return new LocalFilePathPolicy(properties);
    }

    private static LogSource source(Path path) {
        String json = "{\"logFilePath\":\"" + path.toString().replace("\\", "\\\\") + "\"}";
        return LogSource.builder().id(1L).name("f").type(LogSourceType.LOCAL_FILE).config(json).build();
    }
}
