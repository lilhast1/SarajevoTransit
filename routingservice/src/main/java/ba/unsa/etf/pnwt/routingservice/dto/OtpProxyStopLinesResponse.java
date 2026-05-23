package ba.unsa.etf.pnwt.routingservice.dto;

import java.util.List;

public class OtpProxyStopLinesResponse {
    private String stopName;
    private List<Line> lines;

    public String getStopName() { return stopName; }
    public void setStopName(String stopName) { this.stopName = stopName; }
    public List<Line> getLines() { return lines; }
    public void setLines(List<Line> lines) { this.lines = lines; }

    public static class Line {
        private String shortName;
        private String longName;
        private String mode;
        private String agencyName;
        private String gtfsId;

        public String getShortName() { return shortName; }
        public void setShortName(String shortName) { this.shortName = shortName; }
        public String getLongName() { return longName; }
        public void setLongName(String longName) { this.longName = longName; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getAgencyName() { return agencyName; }
        public void setAgencyName(String agencyName) { this.agencyName = agencyName; }
        public String getGtfsId() { return gtfsId; }
        public void setGtfsId(String gtfsId) { this.gtfsId = gtfsId; }
    }
}
