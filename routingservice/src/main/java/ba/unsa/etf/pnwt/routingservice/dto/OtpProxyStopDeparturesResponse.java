package ba.unsa.etf.pnwt.routingservice.dto;

import java.util.List;

public class OtpProxyStopDeparturesResponse {
    private String stopName;
    private List<Departure> departures;

    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public List<Departure> getDepartures() { return departures; }
    public void setDepartures(List<Departure> departures) { this.departures = departures; }

    public static class Departure {
        private Long realtimeDeparture;
        private Long scheduledDeparture;
        private String realtimeState;
        private String headsign;
        private String lineShortName;
        private String lineMode;
        private String gtfsId;

        public Long getRealtimeDeparture() { return realtimeDeparture; }
        public void setRealtimeDeparture(Long realtimeDeparture) { this.realtimeDeparture = realtimeDeparture; }
        public Long getScheduledDeparture() { return scheduledDeparture; }
        public void setScheduledDeparture(Long scheduledDeparture) { this.scheduledDeparture = scheduledDeparture; }
        public String getRealtimeState() { return realtimeState; }
        public void setRealtimeState(String realtimeState) { this.realtimeState = realtimeState; }
        public String getHeadsign() { return headsign; }
        public void setHeadsign(String headsign) { this.headsign = headsign; }
        public String getLineShortName() { return lineShortName; }
        public void setLineShortName(String lineShortName) { this.lineShortName = lineShortName; }
        public String getLineMode() { return lineMode; }
        public void setLineMode(String lineMode) { this.lineMode = lineMode; }
        public String getGtfsId() { return gtfsId; }
        public void setGtfsId(String gtfsId) { this.gtfsId = gtfsId; }
    }
}
