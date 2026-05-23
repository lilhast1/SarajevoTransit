package com.sarajevotransit.otpproxyservice.dto;

import java.util.List;

public class OtpStopDeparturesGraphQlResponse {
    private Data data;

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public static class Data {
        private Stop stop;
        public Stop getStop() { return stop; }
        public void setStop(Stop stop) { this.stop = stop; }
    }

    public static class Stop {
        private String name;
        private List<StopTime> stoptimesWithoutPatterns;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<StopTime> getStoptimesWithoutPatterns() { return stoptimesWithoutPatterns; }
        public void setStoptimesWithoutPatterns(List<StopTime> stoptimesWithoutPatterns) { this.stoptimesWithoutPatterns = stoptimesWithoutPatterns; }
    }

    public static class StopTime {
        private Long realtimeDeparture;
        private Long scheduledDeparture;
        private String realtimeState;
        private String headsign;
        private Trip trip;

        public Long getRealtimeDeparture() { return realtimeDeparture; }
        public void setRealtimeDeparture(Long realtimeDeparture) { this.realtimeDeparture = realtimeDeparture; }
        public Long getScheduledDeparture() { return scheduledDeparture; }
        public void setScheduledDeparture(Long scheduledDeparture) { this.scheduledDeparture = scheduledDeparture; }
        public String getRealtimeState() { return realtimeState; }
        public void setRealtimeState(String realtimeState) { this.realtimeState = realtimeState; }
        public String getHeadsign() { return headsign; }
        public void setHeadsign(String headsign) { this.headsign = headsign; }
        public Trip getTrip() { return trip; }
        public void setTrip(Trip trip) { this.trip = trip; }
    }

    public static class Trip {
        private Route route;

        public Route getRoute() { return route; }
        public void setRoute(Route route) { this.route = route; }
    }

    public static class Route {
        private String shortName;
        private String mode;
        private String gtfsId;

        public String getShortName() { return shortName; }
        public void setShortName(String shortName) { this.shortName = shortName; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getGtfsId() { return gtfsId; }
        public void setGtfsId(String gtfsId) { this.gtfsId = gtfsId; }
    }
}
