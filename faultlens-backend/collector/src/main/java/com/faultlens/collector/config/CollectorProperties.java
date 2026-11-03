package com.faultlens.collector.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("faultlens.collector")
public class CollectorProperties {
    private K8s k8s = new K8s();
    private Ssh ssh = new Ssh();
    private FileSource file = new FileSource();

    @Data
    public static class K8s {
        private String kubeconfigPath = "~/.kube/config";
        private long reconnectIntervalSeconds = 30;
    }

    @Data
    public static class FileSource {
        /** Directories LOCAL_FILE sources may read from; nothing outside them is accepted. */
        private List<String> allowedDirs = new ArrayList<>(List.of("/var/log"));
    }

    @Data
    public static class Ssh {
        private int connectTimeoutMs = 5000;
        private long retryIntervalSeconds = 10;
    }
}
