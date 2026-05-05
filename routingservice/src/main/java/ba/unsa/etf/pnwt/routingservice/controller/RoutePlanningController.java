package ba.unsa.etf.pnwt.routingservice.controller;

import ba.unsa.etf.pnwt.routingservice.dto.OtpProxyOptimalRouteResponse;
import ba.unsa.etf.pnwt.routingservice.service.OtpProxyClientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;

@Validated
@RestController
@RequestMapping("/api/v1/routes")
public class RoutePlanningController {

    private final OtpProxyClientService otpProxyClientService;

    public RoutePlanningController(OtpProxyClientService otpProxyClientService) {
        this.otpProxyClientService = otpProxyClientService;
    }

    @GetMapping("/optimal")
    public OtpProxyOptimalRouteResponse optimalRoute(@Valid @ModelAttribute OptimalRouteQuery query) {
        return otpProxyClientService.getOptimalRoute(
                query.getFromLat(),
                query.getFromLon(),
                query.getToLat(),
                query.getToLon(),
                query.getModes(),
                query.getArriveBy(),
                query.getDate(),
                query.getTime(),
                query.getMaxWalkDistance(),
                query.getMaxTransfers(),
                query.getWheelchair(),
                query.getNumItineraries()
        );
    }

    public static class OptimalRouteQuery {

        @NotNull
        @Min(-90)
        @Max(90)
        private Double fromLat;

        @NotNull
        @Min(-180)
        @Max(180)
        private Double fromLon;

        @NotNull
        @Min(-90)
        @Max(90)
        private Double toLat;

        @NotNull
        @Min(-180)
        @Max(180)
        private Double toLon;

        private String modes;
        private Boolean arriveBy;

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate date;

        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime time;

        @Min(0)
        private Integer maxWalkDistance;

        @Min(0)
        private Integer maxTransfers;

        private Boolean wheelchair;

        @Min(1)
        private Integer numItineraries;

        public Double getFromLat() {
            return fromLat;
        }

        public void setFromLat(Double fromLat) {
            this.fromLat = fromLat;
        }

        public Double getFromLon() {
            return fromLon;
        }

        public void setFromLon(Double fromLon) {
            this.fromLon = fromLon;
        }

        public Double getToLat() {
            return toLat;
        }

        public void setToLat(Double toLat) {
            this.toLat = toLat;
        }

        public Double getToLon() {
            return toLon;
        }

        public void setToLon(Double toLon) {
            this.toLon = toLon;
        }

        public String getModes() {
            return modes;
        }

        public void setModes(String modes) {
            this.modes = modes;
        }

        public Boolean getArriveBy() {
            return arriveBy;
        }

        public void setArriveBy(Boolean arriveBy) {
            this.arriveBy = arriveBy;
        }

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public LocalTime getTime() {
            return time;
        }

        public void setTime(LocalTime time) {
            this.time = time;
        }

        public Integer getMaxWalkDistance() {
            return maxWalkDistance;
        }

        public void setMaxWalkDistance(Integer maxWalkDistance) {
            this.maxWalkDistance = maxWalkDistance;
        }

        public Integer getMaxTransfers() {
            return maxTransfers;
        }

        public void setMaxTransfers(Integer maxTransfers) {
            this.maxTransfers = maxTransfers;
        }

        public Boolean getWheelchair() {
            return wheelchair;
        }

        public void setWheelchair(Boolean wheelchair) {
            this.wheelchair = wheelchair;
        }

        public Integer getNumItineraries() {
            return numItineraries;
        }

        public void setNumItineraries(Integer numItineraries) {
            this.numItineraries = numItineraries;
        }
    }
}
