package com.sarajevotransit.otpproxyservice.dto;

import java.util.List;

public class OtpStopLinesGraphQlResponse {
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
        private List<Route> routes;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public List<Route> getRoutes() { return routes; }
        public void setRoutes(List<Route> routes) { this.routes = routes; }
    }

    public static class Route {
        private String shortName;
        private String longName;
        private String mode;
        private Agency agency;
        private String gtfsId;

        public String getShortName() { return shortName; }
        public void setShortName(String shortName) { this.shortName = shortName; }
        public String getLongName() { return longName; }
        public void setLongName(String longName) { this.longName = longName; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public Agency getAgency() { return agency; }
        public void setAgency(Agency agency) { this.agency = agency; }
        public String getGtfsId() { return gtfsId; }
        public void setGtfsId(String gtfsId) { this.gtfsId = gtfsId; }
    }

    public static class Agency {
        private String name;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
