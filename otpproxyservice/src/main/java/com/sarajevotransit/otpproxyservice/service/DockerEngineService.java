package com.sarajevotransit.otpproxyservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

@Service
public class DockerEngineService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerEngineService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int CONTAINER_WAIT_TIMEOUT_SECONDS = 600;

    private final UnixDomainSocketAddress dockerSocket;
    private final String network;
    private final String otpImage;

    public DockerEngineService(
            @Value("${docker.socket-path:/var/run/docker.sock}") String socketPath,
            @Value("${docker.network:sarajevo-transit-net}") String network,
            @Value("${docker.otp-image:opentripplanner/opentripplanner:latest}") String otpImage) {
        this.network = network;
        this.otpImage = otpImage;
        this.dockerSocket = UnixDomainSocketAddress.of(socketPath);
        LOGGER.info("DockerEngineService initialized: socket={}, network={}, image={}", socketPath, network, otpImage);
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void runOtpBuild(String otpDataDir) {
        String containerName = "otp-build-" + System.currentTimeMillis();
        try {
            LOGGER.info("Creating otp-build container: {}", containerName);

            ObjectNode config = MAPPER.createObjectNode();
            config.put("Image", otpImage);
            config.put("AttachStdout", true);
            config.put("AttachStderr", true);

            ArrayNode cmd = config.putArray("Cmd");
            cmd.add("--build");
            cmd.add("--save");

            ObjectNode hostConfig = config.putObject("HostConfig");
            ArrayNode binds = hostConfig.putArray("Binds");
            String hostOtpData = resolveHostPath(otpDataDir);
            binds.add(hostOtpData + ":/var/opentripplanner");

            String createResponse = httpPost("/containers/create?name=" + containerName, config.toString());
            LOGGER.debug("Docker create response: {}", createResponse);
            JsonNode createNode = MAPPER.readTree(createResponse);
            JsonNode idNode = createNode.get("Id");
            if (idNode == null) {
                throw new IOException("Docker create response missing 'Id': " + createResponse);
            }
            String containerId = idNode.asText();
            LOGGER.info("Container created: {}", containerId.substring(0, 12));

            httpPost("/containers/" + containerId + "/start", "");
            LOGGER.info("Container started, waiting for completion...");

            httpPost("/containers/" + containerId + "/wait", "");
            LOGGER.info("OTP build container finished");

            httpDelete("/containers/" + containerName + "?v=false");
            LOGGER.info("Build container removed");
        } catch (Exception e) {
            cleanupContainer(containerName);
            throw new RuntimeException("Failed to run OTP build via Docker API", e);
        }
    }

    public void startContainer(String containerName) {
        try {
            JsonNode inspect = inspectContainer(containerName);
            if (inspect != null) {
                boolean running = inspect.get("State").get("Running").asBoolean();
                if (running) {
                    LOGGER.info("Container {} is already running, restarting to pick up new graph", containerName);
                    httpPost("/containers/" + containerName + "/stop?t=10", "");
                    LOGGER.info("Container {} stopped for restart", containerName);
                }
                LOGGER.info("Starting container: {}", containerName);
                httpPost("/containers/" + containerName + "/start", "");
                LOGGER.info("Container {} started", containerName);
                return;
            }
        } catch (NotFoundException e) {
            LOGGER.info("Container {} not found, creating...", containerName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inspect container " + containerName, e);
        }

        createAndStartOtpContainer(containerName);
    }

    public void stopContainer(String containerName) {
        try {
            JsonNode inspect = inspectContainer(containerName);
            if (inspect == null || !inspect.get("State").get("Running").asBoolean()) {
                LOGGER.info("Container {} is not running, skipping stop", containerName);
                return;
            }
            LOGGER.info("Stopping container: {}", containerName);
            httpPost("/containers/" + containerName + "/stop?t=10", "");
            LOGGER.info("Container {} stopped", containerName);
        } catch (NotFoundException e) {
            LOGGER.warn("Container {} not found, nothing to stop", containerName);
        } catch (Exception e) {
            LOGGER.error("Failed to stop container {}: {}", containerName, e.getMessage());
        }
    }

    // ── Container creation ────────────────────────────────────────────────────

    private void createAndStartOtpContainer(String containerName) {
        try {
            String color = containerName.contains("blue") ? "BLUE" : "GREEN";
            int hostPort = "BLUE".equals(color) ? 18080 : 18081;

            ObjectNode config = MAPPER.createObjectNode();
            config.put("Image", otpImage);
            config.put("AttachStdout", true);
            config.put("AttachStderr", true);

            ArrayNode cmd = config.putArray("Cmd");
            cmd.add("--load");
            cmd.add("--serve");

            ObjectNode hostConfig = config.putObject("HostConfig");
            ArrayNode binds = hostConfig.putArray("Binds");
            String hostOtpData = resolveHostPath("/app/otp-data");
            binds.add(hostOtpData + ":/var/opentripplanner");

            ObjectNode portBindings = hostConfig.putObject("PortBindings");
            ObjectNode hostPorts = portBindings.putObject("8080/tcp");
            ArrayNode hostPortArr = hostPorts.putArray("HostPort");
            hostPortArr.add(String.valueOf(hostPort));

            hostConfig.put("RestartPolicy", "no");

            ObjectNode networkingConfig = config.putObject("NetworkingConfig");
            ObjectNode endpointsConfig = networkingConfig.putObject("EndpointsConfig");
            endpointsConfig.putObject(network);

            String createResponse = httpPost("/containers/create?name=" + containerName, config.toString());
            LOGGER.debug("Docker create response: {}", createResponse);
            JsonNode createNode = MAPPER.readTree(createResponse);
            JsonNode idNode = createNode.get("Id");
            if (idNode == null) {
                throw new IOException("Docker create response missing 'Id': " + createResponse);
            }
            String containerId = idNode.asText();
            LOGGER.info("Container {} created: {}", containerName, containerId.substring(0, 12));

            httpPost("/containers/" + containerName + "/start", "");
            LOGGER.info("Container {} started on port {}", containerName, hostPort);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create and start OTP container " + containerName, e);
        }
    }

    // ── HTTP over Unix socket ─────────────────────────────────────────────────

    private JsonNode inspectContainer(String name) throws IOException {
        try {
            String response = httpGet("/containers/" + name + "/json");
            return MAPPER.readTree(response);
        } catch (NotFoundException e) {
            return null;
        }
    }

    private String httpGet(String path) throws IOException {
        return executeRawHttp("GET", path, null);
    }

    private String httpPost(String path, String body) throws IOException {
        return executeRawHttp("POST", path, body);
    }

    private String httpDelete(String path) throws IOException {
        return executeRawHttp("DELETE", path, null);
    }

    private String executeRawHttp(String method, String path, String body) throws IOException {
        try (SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX)) {
            channel.connect(dockerSocket);

            OutputStream out = Channels.newOutputStream(channel);

            StringBuilder request = new StringBuilder();
            request.append(method).append(" ").append(path).append(" HTTP/1.1\r\n");
            request.append("Host: localhost\r\n");

            byte[] bodyBytes = null;
            if (body != null && !body.isEmpty()) {
                bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                request.append("Content-Type: application/json\r\n");
                request.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
            }
            request.append("Connection: close\r\n");
            request.append("\r\n");

            out.write(request.toString().getBytes(StandardCharsets.UTF_8));
            if (bodyBytes != null) {
                out.write(bodyBytes);
            }
            out.flush();

            InputStream in = Channels.newInputStream(channel);
            byte[] rawResponse = in.readAllBytes();
            String responseStr = new String(rawResponse, StandardCharsets.UTF_8);

            int headerEnd = responseStr.indexOf("\r\n\r\n");
            if (headerEnd < 0) {
                throw new IOException("Invalid HTTP response from Docker API");
            }

            String headersSection = responseStr.substring(0, headerEnd);
            String statusLine = headersSection.substring(0, headersSection.indexOf("\r\n"));
            String responseBody = responseStr.substring(headerEnd + 4);

            boolean isChunked = headersSection.toLowerCase().contains("transfer-encoding: chunked");
            if (isChunked) {
                responseBody = decodeChunked(responseBody);
            }

            int statusCode = Integer.parseInt(statusLine.split(" ")[1]);

            if (statusCode == 404) {
                throw new NotFoundException("Resource not found (HTTP 404): " + path);
            }
            if (statusCode >= 400) {
                throw new IOException("Docker API error " + statusCode + " on " + method + " " + path + ": " + responseBody);
            }

            return responseBody;
        } catch (NotFoundException e) {
            throw e;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to communicate with Docker daemon at " + dockerSocket, e);
        }
    }

    private static String decodeChunked(String raw) {
        StringBuilder decoded = new StringBuilder();
        int pos = 0;
        while (pos < raw.length()) {
            int crlf = raw.indexOf("\r\n", pos);
            if (crlf < 0) break;
            String hexSize = raw.substring(pos, crlf).trim();
            if (hexSize.isEmpty()) { pos = crlf + 2; continue; }
            int chunkSize;
            try {
                chunkSize = Integer.parseInt(hexSize, 16);
            } catch (NumberFormatException e) {
                break;
            }
            if (chunkSize == 0) break;
            int dataStart = crlf + 2;
            int dataEnd = dataStart + chunkSize;
            if (dataEnd <= raw.length()) {
                decoded.append(raw, dataStart, dataEnd);
            }
            pos = dataEnd + 2;
        }
        return decoded.toString();
    }

    // ── Host path resolution ──────────────────────────────────────────────────

    private String resolveHostPath(String containerPath) {
        try {
            String containerId = readContainerId();
            if (containerId == null) {
                LOGGER.warn("Could not determine container ID, falling back to container path: {}", containerPath);
                return containerPath;
            }

            LOGGER.info("Inspecting container {} to resolve host path for {}", containerId.substring(0, Math.min(12, containerId.length())), containerPath);
            JsonNode inspect = inspectContainer(containerId);
            if (inspect != null) {
                JsonNode mounts = inspect.get("Mounts");
                if (mounts != null && mounts.isArray()) {
                    for (JsonNode mount : mounts) {
                        String dest = mount.get("Destination") != null ? mount.get("Destination").asText() : "";
                        if (containerPath.equals(dest)) {
                            String hostPath = mount.get("Source").asText();
                            LOGGER.info("Resolved host path: {} -> {}", containerPath, hostPath);
                            return hostPath;
                        }
                    }
                }
                LOGGER.warn("Mount for '{}' not found in container mounts", containerPath);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to resolve host path for {}: {}", containerPath, e.getMessage());
        }
        LOGGER.info("Using container path as-is: {}", containerPath);
        return containerPath;
    }

    private String readContainerId() {
        try {
            String hostname = System.getenv("HOSTNAME");
            if (hostname != null && !hostname.isEmpty()) {
                return hostname;
            }
        } catch (Exception ignored) {}
        try {
            byte[] cgroupBytes = java.nio.file.Files.readAllBytes(java.nio.file.Path.of("/proc/self/cgroup"));
            String cgroup = new String(cgroupBytes, StandardCharsets.UTF_8);
            for (String line : cgroup.split("\n")) {
                int idx = line.lastIndexOf("docker-");
                if (idx >= 0) {
                    String id = line.substring(idx + 7).replace(".scope", "").trim();
                    if (!id.isEmpty()) return id;
                }
                idx = line.lastIndexOf("docker/");
                if (idx >= 0) {
                    String id = line.substring(idx + 7).trim();
                    if (!id.isEmpty()) return id;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void cleanupContainer(String containerName) {
        try {
            httpDelete("/containers/" + containerName + "?v=true");
            LOGGER.info("Cleaned up container: {}", containerName);
        } catch (Exception e) {
            LOGGER.warn("Failed to cleanup container {}: {}", containerName, e.getMessage());
        }
    }

    public static class NotFoundException extends IOException {
        public NotFoundException(String message) {
            super(message);
        }
    }
}
