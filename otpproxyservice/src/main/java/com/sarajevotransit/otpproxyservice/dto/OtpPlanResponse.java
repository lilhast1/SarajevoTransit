package com.sarajevotransit.otpproxyservice.dto;

import java.util.List;

public class OtpPlanResponse {

    private Plan plan;

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public static class Plan {

        private List<Itinerary> itineraries;

        public List<Itinerary> getItineraries() {
            return itineraries;
        }

        public void setItineraries(List<Itinerary> itineraries) {
            this.itineraries = itineraries;
        }
    }

    public static class Itinerary {

        private Long duration;
        private Double walkDistance;
        private Integer numberOfTransfers;
        private List<Leg> legs;

        public Long getDuration() {
            return duration;
        }

        public void setDuration(Long duration) {
            this.duration = duration;
        }

        public Double getWalkDistance() {
            return walkDistance;
        }

        public void setWalkDistance(Double walkDistance) {
            this.walkDistance = walkDistance;
        }

        public Integer getNumberOfTransfers() {
            return numberOfTransfers;
        }

        public void setNumberOfTransfers(Integer numberOfTransfers) {
            this.numberOfTransfers = numberOfTransfers;
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
        private Place from;
        private Place to;
        private LegTime start;
        private LegTime end;
        private Double distance;

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public Place getFrom() {
            return from;
        }

        public void setFrom(Place from) {
            this.from = from;
        }

        public Place getTo() {
            return to;
        }

        public void setTo(Place to) {
            this.to = to;
        }

        public LegTime getStart() {
            return start;
        }

        public void setStart(LegTime start) {
            this.start = start;
        }

        public LegTime getEnd() {
            return end;
        }

        public void setEnd(LegTime end) {
            this.end = end;
        }

        public Double getDistance() {
            return distance;
        }

        public void setDistance(Double distance) {
            this.distance = distance;
        }
    }

    public static class Place {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class LegTime {

        private String scheduledTime;

        public String getScheduledTime() {
            return scheduledTime;
        }

        public void setScheduledTime(String scheduledTime) {
            this.scheduledTime = scheduledTime;
        }
    }
}
