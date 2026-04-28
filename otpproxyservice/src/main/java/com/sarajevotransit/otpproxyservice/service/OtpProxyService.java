package com.sarajevotransit.otpproxyservice.service;

import com.sarajevotransit.otpproxyservice.dto.OtpGraphQlResponse;
import com.sarajevotransit.otpproxyservice.dto.StopsCountResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OtpProxyService {

    private static final String STOPS_QUERY = "{ stops { id } }";

    private final RestClient restClient;

    @Value("${otp.base-url}")
    private String otpBaseUrl;

    public OtpProxyService(RestClient restClient) {
        this.restClient = restClient;
    }

    public StopsCountResponse fetchStopsCount() {
        OtpGraphQlResponse response = restClient.post()
                .uri(otpBaseUrl + "/otp/routers/default/index/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", STOPS_QUERY))
                .retrieve()
                .body(OtpGraphQlResponse.class);

        int count = 0;
        if (response != null && response.data() != null && response.data().stops() != null) {
            count = response.data().stops().size();
        }

        return new StopsCountResponse(count, "otp-proxy");
    }
}
