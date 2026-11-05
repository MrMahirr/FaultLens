package com.faultlens.collector.adapter;

import com.faultlens.collector.config.CollectorProperties;
import com.faultlens.common.exception.CollectorException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Restricts LOCAL_FILE sources to configured directories
 * ({@code faultlens.collector.file.allowed-dirs}).
 *
 * <p>Without this any authenticated user could create a source for {@code /etc/passwd},
 * {@code .env} or a private key and read it through the log viewer. Paths are normalized
 * ({@code ..} removed) and, when they exist, resolved to their real location so symlinks cannot
 * point outside an allowed directory.
 */
@Component
public class LocalFilePathPolicy {
    static final String ERROR_CODE = "LOCAL_FILE_PATH_NOT_ALLOWED";

    private final List<Path> allowedDirs;

    public LocalFilePathPolicy(CollectorProperties properties) {
        this.allowedDirs = properties.getFile().getAllowedDirs().stream()
                .filter(dir -> dir != null && !dir.isBlank())
                .map(dir -> canonical(Path.of(dir.trim())))
                .toList();
    }

    /**
     * Returns the canonical path when it lies inside an allowed directory.
     *
     * @throws CollectorException with code {@value #ERROR_CODE} otherwise
     */
    public Path requireAllowed(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            throw new CollectorException("Local file path is required", ERROR_CODE);
        }
        Path candidate;
        try {
            candidate = canonical(Path.of(rawPath.trim()));
        } catch (InvalidPathException ex) {
            throw new CollectorException("Invalid local file path", ERROR_CODE, ex);
        }
        for (Path allowed : allowedDirs) {
            if (candidate.startsWith(allowed) && !candidate.equals(allowed)) {
                return candidate;
            }
        }
        throw new CollectorException(
                "Local file path is outside the allowed directories (faultlens.collector.file.allowed-dirs)",
                ERROR_CODE);
    }

    /**
     * Returns true when {@link #requireAllowed(String)} would accept the path.
     */
    public boolean isAllowed(String rawPath) {
        try {
            requireAllowed(rawPath);
            return true;
        } catch (CollectorException ex) {
            return false;
        }
    }

    List<Path> allowedDirs() {
        return allowedDirs;
    }

    /**
     * Absolute + normalized; for existing paths (or their closest existing parent) symlinks are
     * resolved so a link inside an allowed dir cannot escape it.
     */
    private static Path canonical(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        Path existing = normalized;
        while (existing != null && !Files.exists(existing)) {
            existing = existing.getParent();
        }
        if (existing == null) {
            return normalized;
        }
        try {
            return existing.toRealPath().resolve(existing.relativize(normalized)).normalize();
        } catch (IOException ex) {
            return normalized;
        }
    }
}
