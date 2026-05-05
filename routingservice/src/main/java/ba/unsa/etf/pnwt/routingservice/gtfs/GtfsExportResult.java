package ba.unsa.etf.pnwt.routingservice.gtfs;

import java.util.List;

public record GtfsExportResult(
        String fileName,
        byte[] zipBytes,
        int routesCount,
        int tripsCount,
        int stopTimesCount,
        int shapesCount,
        List<String> warnings
) {
}
