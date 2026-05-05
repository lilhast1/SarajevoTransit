package com.sarajevotransit.otpproxyservice.service;

import com.sarajevotransit.otpproxyservice.dto.OtpGraphQlResponse;
import com.sarajevotransit.otpproxyservice.dto.OtpPlanGraphQlResponse;
import com.sarajevotransit.otpproxyservice.dto.OtpPlanResponse;
import com.sarajevotransit.otpproxyservice.dto.OptimalRouteResponse;
import com.sarajevotransit.otpproxyservice.dto.StopsCountResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class OtpProxyService {

    private static final String STOPS_QUERY = "{ stops { id } }";
    private static final String DEFAULT_MODES = "BUS,TRAM,TROLLEYBUS,WALK";
    private static final Pattern MODE_PATTERN = Pattern.compile("[A-Z_]+");

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

    public OptimalRouteResponse fetchOptimalRoute(
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
        String modesValue = modes == null || modes.isBlank() ? DEFAULT_MODES : modes;
        boolean arriveByValue = Boolean.TRUE.equals(arriveBy);
        LocalDate dateValue = date == null ? LocalDate.now() : date;
        LocalTime timeValue = time == null ? LocalTime.now().withNano(0) : time;
        int maxWalkDistanceValue = maxWalkDistance == null ? 1000 : maxWalkDistance;
        int maxTransfersValue = maxTransfers == null ? 2 : maxTransfers;
        boolean wheelchairValue = Boolean.TRUE.equals(wheelchair);
        int numItinerariesValue = numItineraries == null ? 3 : numItineraries;

        String query = buildPlanGraphQlQuery(
                fromLat,
                fromLon,
                toLat,
                toLon,
                modesValue,
                arriveByValue,
                dateValue,
                timeValue,
                maxWalkDistanceValue,
                maxTransfersValue,
                wheelchairValue,
                numItinerariesValue
        );

        OtpPlanGraphQlResponse graphQlResponse = restClient.post()
                .uri(otpBaseUrl + "/otp/routers/default/index/graphql")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", query))
                .retrieve()
                .body(OtpPlanGraphQlResponse.class);

        OtpPlanResponse response = graphQlResponse != null && graphQlResponse.getData() != null
                ? new OtpPlanResponse()
                : null;
        if (response != null) {
            response.setPlan(graphQlResponse.getData().getPlan());
        }

        return mapToOptimalRouteResponse(response, numItinerariesValue);
    }

    private OptimalRouteResponse mapToOptimalRouteResponse(OtpPlanResponse response, int requestedItineraries) {
        OptimalRouteResponse mapped = new OptimalRouteResponse();
        mapped.setRequestedItineraries(requestedItineraries);
        mapped.setSource("otp-proxy");

        List<OptimalRouteResponse.Itinerary> itineraries = new ArrayList<>();
        if (response != null && response.getPlan() != null && response.getPlan().getItineraries() != null) {
            for (OtpPlanResponse.Itinerary itinerary : response.getPlan().getItineraries()) {
                OptimalRouteResponse.Itinerary mappedItinerary = new OptimalRouteResponse.Itinerary();
                mappedItinerary.setDurationSeconds(itinerary.getDuration());
                mappedItinerary.setWalkDistanceMeters(itinerary.getWalkDistance());
                mappedItinerary.setTransfers(itinerary.getNumberOfTransfers());

                List<OptimalRouteResponse.Leg> legs = new ArrayList<>();
                if (itinerary.getLegs() != null) {
                    for (OtpPlanResponse.Leg leg : itinerary.getLegs()) {
                        OptimalRouteResponse.Leg mappedLeg = new OptimalRouteResponse.Leg();
                        mappedLeg.setMode(leg.getMode());
                        mappedLeg.setFromName(leg.getFrom() != null ? leg.getFrom().getName() : null);
                        mappedLeg.setToName(leg.getTo() != null ? leg.getTo().getName() : null);
                        mappedLeg.setStartTime(toEpochMillis(leg.getStart() != null ? leg.getStart().getScheduledTime() : null));
                        mappedLeg.setEndTime(toEpochMillis(leg.getEnd() != null ? leg.getEnd().getScheduledTime() : null));
                        mappedLeg.setDistanceMeters(leg.getDistance());
                        legs.add(mappedLeg);
                    }
                }
                mappedItinerary.setLegs(legs);
                itineraries.add(mappedItinerary);
            }
        }

        mapped.setItineraries(itineraries);
        return mapped;
    }

    private String buildPlanGraphQlQuery(
            double fromLat,
            double fromLon,
            double toLat,
            double toLon,
            String modes,
            boolean arriveBy,
            LocalDate date,
            LocalTime time,
            int maxWalkDistance,
            int maxTransfers,
            boolean wheelchair,
            int numItineraries
    ) {
        String transportModes = buildTransportModesGraphQlInput(modes);

        return "query {"
                + " plan(" 
                + "from:{lat:" + fromLat + ",lon:" + fromLon + "},"
                + "to:{lat:" + toLat + ",lon:" + toLon + "},"
                + "transportModes:" + transportModes + ","
                + "arriveBy:" + arriveBy + ","
                + "date:\"" + date + "\","
                + "time:\"" + time.withNano(0) + "\","
                + "maxWalkDistance:" + maxWalkDistance + ","
                + "maxTransfers:" + maxTransfers + ","
                + "wheelchair:" + wheelchair + ","
                + "numItineraries:" + numItineraries
                + ") {"
                + " itineraries {"
                + " duration walkDistance numberOfTransfers"
                + " legs { mode from { name } to { name } start { scheduledTime } end { scheduledTime } distance }"
                + " }"
                + " }"
                + "}";
    }

    private String buildTransportModesGraphQlInput(String modes) {
        List<String> entries = new ArrayList<>();
        String[] tokens = modes.split(",");
        for (String token : tokens) {
            String mode = token.trim().toUpperCase();
            if (mode.isEmpty() || !MODE_PATTERN.matcher(mode).matches()) {
                continue;
            }
            entries.add("{mode:" + mode + "}");
        }
        if (entries.isEmpty()) {
            entries.add("{mode:BUS}");
            entries.add("{mode:TRAM}");
            entries.add("{mode:TROLLEYBUS}");
            entries.add("{mode:WALK}");
        }
        return "[" + String.join(",", entries) + "]";
    }

    private Long toEpochMillis(String isoOffsetDateTime) {
        if (isoOffsetDateTime == null || isoOffsetDateTime.isBlank()) {
            return null;
        }
        return OffsetDateTime.parse(isoOffsetDateTime).toInstant().toEpochMilli();
    }
}
