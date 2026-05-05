package com.sarajevotransit.otpproxyservice.dto;

import java.util.ArrayList;
import java.util.List;

public class OptimalRouteResponse {

    private List<Itinerary> itineraries = new ArrayList<>();
    private Integer requestedItineraries;
    private String source;

    public List<Itinerary> getItineraries() {
        return itineraries;
    }

    public void setItineraries(List<Itinerary> itineraries) {
        this.itineraries = itineraries;
    }

    public Integer getRequestedItineraries() {
        return requestedItineraries;
    }

    public void setRequestedItineraries(Integer requestedItineraries) {
        this.requestedItineraries = requestedItineraries;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public static class Itinerary {

        private Long durationSeconds;
        private Double walkDistanceMeters;
        private Integer transfers;
        private List<Leg> legs = new ArrayList<>();

        public Long getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(Long durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        public Double getWalkDistanceMeters() {
            return walkDistanceMeters;
        }

        public void setWalkDistanceMeters(Double walkDistanceMeters) {
            this.walkDistanceMeters = walkDistanceMeters;
        }

        public Integer getTransfers() {
            return transfers;
        }

        public void setTransfers(Integer transfers) {
            this.transfers = transfers;
        }

        public List<Leg> getLegs() {
            return legs;
        }

        public void setLegs(List<Leg> legs) {
            this.legs = legs;
        }
    }

    public static class Leg {

        private String mode;
        private String fromName;
        private String toName;
        private Long startTime;
        private Long endTime;
        private Double distanceMeters;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getFromName() {
            return fromName;
        }

        public void setFromName(String fromName) {
            this.fromName = fromName;
        }

        public String getToName() {
            return toName;
        }

        public void setToName(String toName) {
            this.toName = toName;
        }

        public Long getStartTime() {
            return startTime;
        }

        public void setStartTime(Long startTime) {
            this.startTime = startTime;
        }

        public Long getEndTime() {
            return endTime;
        }

        public void setEndTime(Long endTime) {
            this.endTime = endTime;
        }

        public Double getDistanceMeters() {
            return distanceMeters;
        }

        public void setDistanceMeters(Double distanceMeters) {
            this.distanceMeters = distanceMeters;
        }
    }
}
