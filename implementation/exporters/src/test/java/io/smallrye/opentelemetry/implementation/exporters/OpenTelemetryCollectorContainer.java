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

public class OpenTelemetryCollectorContainer extends GenericContainer<OpenTelemetryCollectorContainer> {
    public static final int HEALTH_CHECK_PORT = 13133;
    public static final int OTLP_GRPC_PORT = 4317;
    public static final int OTLP_HTTP_PORT = 4318;
    public static final String OTEL_COLLECTOR_CONFIG_YAML = "/etc/otel-collector-config.yaml";
    private static final String imageName = "otel/opentelemetry-collector";
    private static final String imageVersion = "0.89.0";

    private String otlpGrpcEndpoint;
    private String otlpHttpEndpoint;

    public OpenTelemetryCollectorContainer() {
        super(DockerImageName.parse(imageName + ":" + imageVersion));
        setExposedPorts(Lists.newArrayList(OTLP_HTTP_PORT, OTLP_GRPC_PORT, HEALTH_CHECK_PORT));
        setWaitStrategy(
                new WaitAllStrategy()
                        .withStrategy(Wait.forHttp("/").forPort(HEALTH_CHECK_PORT))
                        //                        .withStrategy(Wait.forHttp("/v1/metrics").forPort(HTTP_OTLP_PORT).forStatusCode(405))
                        .withStartupTimeout(Duration.ofSeconds(5)));
        setStartupAttempts(5);
    }

    @Override
    public OpenTelemetryCollectorContainer withExposedPorts(Integer... ports) {
        getExposedPorts().addAll(Lists.newArrayList(ports));
        return this;
    }

    @Override
    public void start() {
        super.start();
        otlpGrpcEndpoint = "http://localhost:" + getMappedPort(OTLP_GRPC_PORT);
        otlpHttpEndpoint = "http://localhost:" + getMappedPort(OTLP_HTTP_PORT);
    }

    public String getOtlpGrpcEndpoint() {
        return otlpGrpcEndpoint;
    }

    public String getOtlpHttpEndpoint() {
        return otlpHttpEndpoint;
    }
}
