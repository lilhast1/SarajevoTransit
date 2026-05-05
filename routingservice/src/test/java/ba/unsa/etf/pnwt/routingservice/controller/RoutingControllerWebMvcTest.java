package ba.unsa.etf.pnwt.routingservice.controller;

import ba.unsa.etf.pnwt.routingservice.dto.LineResponse;
import ba.unsa.etf.pnwt.routingservice.dto.OtpProxyOptimalRouteResponse;
import ba.unsa.etf.pnwt.routingservice.exception.GlobalExceptionHandler;
import ba.unsa.etf.pnwt.routingservice.exception.ResourceNotFoundException;
import ba.unsa.etf.pnwt.routingservice.service.OtpProxyClientService;
import ba.unsa.etf.pnwt.routingservice.service.RoutingCrudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoutingControllerWebMvcTest {

    private MockMvc mockMvc;

    @Mock
    private RoutingCrudService routingCrudService;

    @Mock
    private OtpProxyClientService otpProxyClientService;

    @InjectMocks
    private RoutingController routingController;

    @InjectMocks
    private DiscoveryTestController discoveryTestController;

    @InjectMocks
    private RoutePlanningController routePlanningController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(routingController, discoveryTestController, routePlanningController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getLinesReturnsOk() throws Exception {
        LineResponse response = new LineResponse();
        response.setId(1);
        response.setName("Line 1");

        when(routingCrudService.getLines(null, null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/lines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Line 1"));
    }

    @Test
    void createLineWithInvalidPayloadReturnsValidationError() throws Exception {
        String payload = "{\"vehicleTypeId\":2,\"isActive\":true}";

        mockMvc.perform(post("/api/v1/lines")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void getLineWhenMissingReturnsNotFoundError() throws Exception {
        when(routingCrudService.getLine(999)).thenThrow(new ResourceNotFoundException("Line not found: id=999"));

        mockMvc.perform(get("/api/v1/lines/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("not_found"))
                .andExpect(jsonPath("$.message").value("Line not found: id=999"));
    }

    @Test
    void optimalRouteReturnsOkWithFilters() throws Exception {
        OtpProxyOptimalRouteResponse routeResponse = new OtpProxyOptimalRouteResponse();
        routeResponse.setRequestedItineraries(2);
        routeResponse.setSource("otp-proxy");

        OtpProxyOptimalRouteResponse.Itinerary itinerary = new OtpProxyOptimalRouteResponse.Itinerary();
        itinerary.setDurationSeconds(1200L);
        itinerary.setTransfers(1);
        routeResponse.setItineraries(List.of(itinerary));

        when(otpProxyClientService.getOptimalRoute(
                43.85,
                18.36,
                43.86,
                18.42,
                "BUS,TRAM",
                false,
                LocalDate.of(2026, 5, 4),
                LocalTime.of(12, 30),
                900,
                1,
                false,
                2
        )).thenReturn(routeResponse);

        mockMvc.perform(get("/api/v1/routes/optimal")
                        .param("fromLat", "43.85")
                        .param("fromLon", "18.36")
                        .param("toLat", "43.86")
                        .param("toLon", "18.42")
                        .param("modes", "BUS,TRAM")
                        .param("arriveBy", "false")
                        .param("date", "2026-05-04")
                        .param("time", "12:30")
                        .param("maxWalkDistance", "900")
                        .param("maxTransfers", "1")
                        .param("wheelchair", "false")
                        .param("numItineraries", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("otp-proxy"))
                .andExpect(jsonPath("$.requestedItineraries").value(2))
                .andExpect(jsonPath("$.itineraries[0].durationSeconds").value(1200));
    }

    @Test
    void optimalRouteWithInvalidLatitudeReturnsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/routes/optimal")
                        .param("fromLat", "120")
                        .param("fromLon", "18.36")
                        .param("toLat", "43.86")
                        .param("toLon", "18.42"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"));
    }

    @Test
    void optimalRouteWhenOtpProxyFailsReturnsInternalError() throws Exception {
        when(otpProxyClientService.getOptimalRoute(
                43.85,
                18.36,
                43.86,
                18.42,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )).thenThrow(new RestClientException("otp unavailable"));

        mockMvc.perform(get("/api/v1/routes/optimal")
                        .param("fromLat", "43.85")
                        .param("fromLon", "18.36")
                        .param("toLat", "43.86")
                        .param("toLon", "18.42"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("internal_error"));
    }
}
