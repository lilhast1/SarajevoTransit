package ba.unsa.etf.pnwt.routingservice.service;

import ba.unsa.etf.pnwt.routingservice.dto.OtpProxyOptimalRouteResponse;
import ba.unsa.etf.pnwt.routingservice.dto.OtpProxyStopsCountResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.LocalTime;

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

    public OtpProxyOptimalRouteResponse getOptimalRoute(
            double fromLat,
            double fromLon,
            double toLat,
            double toLon,
            String modes,
            Boolean arriveBy,
            LocalDate date,
            LocalTime time,
            Integer maxWalkDistance,
            Integer maxTransfers,
            Boolean wheelchair,
            Integer numItineraries
    ) {
        String uri = UriComponentsBuilder.fromUriString("http://otp-proxy")
                .path("/api/v1/proxy/optimal-route")
                .queryParam("fromLat", fromLat)
                .queryParam("fromLon", fromLon)
                .queryParam("toLat", toLat)
                .queryParam("toLon", toLon)
                .queryParamIfPresent("modes", java.util.Optional.ofNullable(modes))
                .queryParamIfPresent("arriveBy", java.util.Optional.ofNullable(arriveBy))
                .queryParamIfPresent("date", java.util.Optional.ofNullable(date))
                .queryParamIfPresent("time", java.util.Optional.ofNullable(time))
                .queryParamIfPresent("maxWalkDistance", java.util.Optional.ofNullable(maxWalkDistance))
                .queryParamIfPresent("maxTransfers", java.util.Optional.ofNullable(maxTransfers))
                .queryParamIfPresent("wheelchair", java.util.Optional.ofNullable(wheelchair))
                .queryParamIfPresent("numItineraries", java.util.Optional.ofNullable(numItineraries))
                .build(true)
                .toUriString();

        return loadBalancedRestClientBuilder.build()
                .get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(OtpProxyOptimalRouteResponse.class);
    }
}
