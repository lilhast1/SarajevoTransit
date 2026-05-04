package com.sarajevotransit.apigateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/v3/api-docs")
public class ApiDocsProxyController {

    private static final Map<String, String> EUREKA_ID_MAP = Map.of(
            "userservice",        "userservice",
            "feedbackservice",    "feedbackservice",
            "notificationservice","notifications",
            "vehicleservice",     "vehicle-service",
            "routingservice",     "routingservice",
            "moneyman",           "moneyman"
    );

    private final LoadBalancerClient loadBalancerClient;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ApiDocsProxyController(LoadBalancerClient loadBalancerClient, ObjectMapper objectMapper) {
        this.loadBalancerClient = loadBalancerClient;
        this.restClient = RestClient.create();
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/{service}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getApiDocs(@PathVariable String service, HttpServletRequest request) {
        String eurekaId = EUREKA_ID_MAP.get(service);
        if (eurekaId == null) {
            return ResponseEntity.notFound().build();
        }

        ServiceInstance instance = loadBalancerClient.choose(eurekaId);
        if (instance == null) {
            return ResponseEntity.status(503)
                    .body("{\"error\": \"No instances available for: " + service + "\"}");
        }

        String url = instance.getUri() + "/v3/api-docs";
        try {
            String docs = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            String gatewayUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
            docs = rewriteServers(docs, gatewayUrl);

            return ResponseEntity.ok(docs);
        } catch (Exception e) {
            return ResponseEntity.status(503)
                    .body("{\"error\": \"Failed to fetch docs for: " + service + "\", \"detail\": \"" + e.getMessage() + "\"}");
        }
    }

    private String rewriteServers(String docs, String gatewayUrl) {
        try {
            ObjectNode root = (ObjectNode) objectMapper.readTree(docs);
            ArrayNode servers = objectMapper.createArrayNode();
            ObjectNode server = objectMapper.createObjectNode();
            server.put("url", gatewayUrl);
            server.put("description", "API Gateway");
            servers.add(server);
            root.set("servers", servers);
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            return docs;
        }
    }
}
