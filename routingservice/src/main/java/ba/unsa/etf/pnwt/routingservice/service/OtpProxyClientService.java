package ba.unsa.etf.pnwt.routingservice.service;

import ba.unsa.etf.pnwt.routingservice.dto.OtpProxyStopsCountResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;

@Service
public class OtpProxyClientService {

    private final RestClient.Builder loadBalancedRestClientBuilder;

    public OtpProxyClientService(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder loadBalancedRestClientBuilder) {
        this.loadBalancedRestClientBuilder = loadBalancedRestClientBuilder;
    }

    public OtpProxyStopsCountResponse getStopsCount() {
        return loadBalancedRestClientBuilder.build()
                .get()
                .uri("http://otp-proxy/api/v1/proxy/stops-count")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OtpProxyStopsCountResponse.class);
    }
}
