package com.sarajevotransit.otpproxyservice.service;

import com.sarajevotransit.otpproxyservice.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;

@Service
public class OtpRebuildListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpRebuildListener.class);
    private static final int HEALTH_POLL_MAX_ATTEMPTS = 60;
    private static final Duration HEALTH_POLL_INTERVAL = Duration.ofSeconds(5);

    private final OtpColorState colorState;
    private final RestClient restClient;
    private final DockerEngineService dockerEngineService;

    @Value("${otp.swap-drain-seconds:30}")
    private int drainSeconds;

    @Value("${routing.base-url}")
    private String routingBaseUrl;

    @Value("${routing.admin-token:}")
    private String adminToken;

    @Value("${otp.data-dir:./otp-data}")
    private String otpDataDir;

    public OtpRebuildListener(OtpColorState colorState, RestClient restClient, DockerEngineService dockerEngineService) {
        this.colorState = colorState;
        this.restClient = restClient;
        this.dockerEngineService = dockerEngineService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void onRebuildTriggered(Map<String, Object> event) {
        String changeType = (String) event.get("changeType");
        if (!"REBUILD_TRIGGERED".equals(changeType)) {
            LOGGER.debug("Ignoring event with changeType={}", changeType);
            return;
        }

        if (colorState.isRebuilding()) {
            LOGGER.warn("Rebuild already in progress, skipping event");
            return;
        }

        String targetColor = colorState.getInactiveColor();
        LOGGER.info("Starting blue-green rebuild: target={}", targetColor);

        try {
            colorState.setRebuilding(true);

            updateRoutingStatus("BUILDING_GRAPH");
            downloadGtfs();
            buildGraph();

            updateRoutingStatus("STARTING_CONTAINER");
            dockerStart("otp-" + targetColor.toLowerCase());

            updateRoutingStatus("HEALTH_CHECK");
            healthPoll(targetColor);

            updateRoutingStatus("SWITCHING_TRAFFIC");
            colorState.switchTo(targetColor);
            LOGGER.info("Traffic switched to {}", targetColor);

            updateRoutingStatus("DRAINING");
            Thread.sleep(drainSeconds * 1000L);

            String oldColor = "BLUE".equals(targetColor) ? "GREEN" : "BLUE";
            dockerStop("otp-" + oldColor.toLowerCase());

            callRoutingComplete();
            LOGGER.info("Blue-green rebuild completed successfully: active={}", targetColor);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Rebuild interrupted", e);
            callRoutingFail(e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Rebuild failed", e);
            callRoutingFail(e.getMessage());
        } finally {
            colorState.setRebuilding(false);
        }
    }

    private void downloadGtfs() {
        LOGGER.info("Downloading GTFS from routingservice...");
        ResponseEntity<byte[]> response = restClient.post()
                .uri(routingBaseUrl + "/api/v1/admin/gtfs/export")
                .header("X-Admin-Token", adminToken)
                .retrieve()
                .toEntity(byte[].class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("GTFS export failed with status: " + response.getStatusCode());
        }

        try {
            Path dataDir = Paths.get(otpDataDir);
            Files.createDirectories(dataDir);
            Path gtfsTarget = dataDir.resolve("gtfs.zip");
            Files.write(gtfsTarget, response.getBody());
            LOGGER.info("GTFS saved to {} ({} bytes)", gtfsTarget, response.getBody().length);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save GTFS file", e);
        }
    }

    private void buildGraph() {
        LOGGER.info("Building OTP graph via Docker Engine API...");
        dockerEngineService.runOtpBuild(otpDataDir);
        LOGGER.info("OTP graph build completed successfully");
    }

    private void dockerStart(String containerName) {
        LOGGER.info("Starting container: {}", containerName);
        dockerEngineService.startContainer(containerName);
        LOGGER.info("Container {} started", containerName);
    }

    private void dockerStop(String containerName) {
        LOGGER.info("Stopping container: {}", containerName);
        dockerEngineService.stopContainer(containerName);
    }

    private void healthPoll(String targetColor) {
        String healthUrl = colorState.getInactiveUrl() + "/otp/";
        LOGGER.info("Health polling {} at {}", targetColor, healthUrl);

        for (int attempt = 1; attempt <= HEALTH_POLL_MAX_ATTEMPTS; attempt++) {
            try {
                ResponseEntity<Void> response = restClient.get()
                        .uri(healthUrl)
                        .retrieve()
                        .toBodilessEntity();

                if (response.getStatusCode().is2xxSuccessful()) {
                    LOGGER.info("Health check passed on attempt {}", attempt);
                    return;
                }
            } catch (Exception e) {
                LOGGER.warn("Health check attempt {}/{} failed: {}", attempt, HEALTH_POLL_MAX_ATTEMPTS, e.getMessage());
            }

            try {
                Thread.sleep(HEALTH_POLL_INTERVAL.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Health check interrupted", e);
            }
        }

        throw new RuntimeException("Health check failed after " + HEALTH_POLL_MAX_ATTEMPTS + " attempts");
    }

    private void updateRoutingStatus(String status) {
        try {
            restClient.post()
                    .uri(routingBaseUrl + "/api/v1/admin/otp-rebuild/status")
                    .header("X-Internal-Token", adminToken)
                    .header("X-Rebuild-Status", status)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            LOGGER.warn("Failed to update routing status to {}: {}", status, e.getMessage());
        }
    }

    private void callRoutingComplete() {
        try {
            restClient.post()
                    .uri(routingBaseUrl + "/api/v1/admin/otp-rebuild/complete")
                    .header("X-Internal-Token", adminToken)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            LOGGER.warn("Failed to call routing rebuild complete: {}", e.getMessage());
        }
    }

    private void callRoutingFail(String errorMessage) {
        try {
            restClient.post()
                    .uri(routingBaseUrl + "/api/v1/admin/otp-rebuild/fail")
                    .header("X-Internal-Token", adminToken)
                    .header("X-Error-Message", errorMessage != null ? errorMessage : "Unknown error")
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            LOGGER.warn("Failed to call routing rebuild fail: {}", e.getMessage());
        }
    }

}
