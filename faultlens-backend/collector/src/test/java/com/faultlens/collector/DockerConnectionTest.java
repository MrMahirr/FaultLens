package com.faultlens.collector;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import org.junit.jupiter.api.Test;

public class DockerConnectionTest {
    @Test
    public void testDocker() {
        String containerId = "df753655324072dabea19dacc67a9ff1f267b54ec8261781d4249ead92c73d86";
        try {
            System.out.println("Testing Default DockerClientBuilder...");
            DockerClient client = DockerClientBuilder.getInstance().build();
            InspectContainerResponse response = client.inspectContainerCmd(containerId).exec();
            System.out.println("Success! Name: " + response.getName());
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("\nTrying with npipe...");
            try {
                DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                        .withDockerHost("npipe:////./pipe/docker_engine")
                        .build();
                DockerClient client2 = DockerClientBuilder.getInstance(config).build();
                InspectContainerResponse response = client2.inspectContainerCmd(containerId).exec();
                System.out.println("npipe Success! Name: " + response.getName());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}
