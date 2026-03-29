package ba.unsa.etf.pnwt.routingservice.importer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ScrapeSnapshot {

    private List<ScrapedLine> lines = new ArrayList<>();

    public List<ScrapedLine> getLines() {
        return lines;
    }

    public void setLines(List<ScrapedLine> lines) {
        this.lines = lines;
    }
}
