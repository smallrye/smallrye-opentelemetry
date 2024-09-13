/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package io.smallrye.opentelemetry.implementation.exporters;

import java.time.Duration;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.shaded.com.google.common.collect.Lists;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class OpenTelemetryCollectorContainer extends GenericContainer<OpenTelemetryCollectorContainer> {
    private static final String imageName = "otel/opentelemetry-collector";
    private static final String imageVersion = "0.103.1";

    private static final int HEALTH_CHECK_PORT = 13133;
    private static final int OTLP_GRPC_PORT = 4317;
    private static final int OTLP_HTTP_PORT = 4318;
    private static final String OTEL_COLLECTOR_CONFIG_YAML = "/etc/otel-collector-config.yaml";

    public OpenTelemetryCollectorContainer() {
        super(DockerImageName.parse(imageName + ":" + imageVersion));

        setExposedPorts(Lists.newArrayList(OTLP_HTTP_PORT, OTLP_GRPC_PORT, HEALTH_CHECK_PORT));
        setWaitStrategy(
                new WaitAllStrategy()
                        .withStrategy(Wait.forHttp("/").forPort(HEALTH_CHECK_PORT))
                        .withStartupTimeout(Duration.ofSeconds(5)));
        setStartupAttempts(5);
        withCopyToContainer(
                MountableFile.forClasspathResource("/otel-collector-config.yaml"), OTEL_COLLECTOR_CONFIG_YAML);
        withCommand("--config " + OTEL_COLLECTOR_CONFIG_YAML);

    }

    @Override
    public void start() {
        super.start();
    }

    @Override
    public OpenTelemetryCollectorContainer withExposedPorts(Integer... ports) {
        getExposedPorts().addAll(Lists.newArrayList(ports));
        return this;
    }

    public String getOtlpGrpcEndpoint() {
        return "http://localhost:" + getMappedPort(OTLP_GRPC_PORT);
    }

    public String getOtlpHttpEndpoint() {
        return "http://localhost:" + getMappedPort(OTLP_HTTP_PORT);
    }
}
