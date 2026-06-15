package ba.unsa.etf.pnwt.routingservice.importer;

import ba.unsa.etf.pnwt.routingservice.importer.model.ScrapedRoutePoint;
import ba.unsa.etf.pnwt.routingservice.importer.model.ScrapedStation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoutingSnapshotImporterTest {

    private final RoutingSnapshotImporter importer = new RoutingSnapshotImporter(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );

    @Test
    void orderStationsByRoutePointsSortsOutOfOrderStationList() {
        List<ScrapedStation> stations = List.of(
                station(371, "Alipasina BUS", 43.859943, 18.411933),
                station(374, "Bolnica Kosevo A", 43.866936, 18.416638),
                station(369, "Dom Armije", 43.857911, 18.424167),
                station(372, "Medicinski fakultet A", 43.865805, 18.413563),
                station(380, "Podhrastovi", 43.868736, 18.423498),
                station(370, "Pozoriste", 43.857651, 18.419943),
                station(378, "Studentski dom A", 43.866974, 18.421771),
                station(376, "Visnjik A", 43.866308, 18.418196)
        );

        List<ScrapedRoutePoint> routePoints = List.of(
                routePoint(488, 43.857911, 18.424167),
                routePoint(488, 43.857651, 18.419943),
                routePoint(489, 43.859943, 18.411933),
                routePoint(490, 43.865805, 18.413563),
                routePoint(491, 43.866936, 18.416638),
                routePoint(492, 43.866308, 18.418196),
                routePoint(493, 43.866974, 18.421771),
                routePoint(494, 43.868736, 18.423498)
        );

        List<Integer> orderedStationIds = importer.orderStationsByRoutePoints(stations, routePoints).stream()
                .map(ScrapedStation::getId)
                .toList();

        assertEquals(List.of(369, 370, 371, 372, 374, 376, 378, 380), orderedStationIds);
    }

    @Test
    void orderStationsByRoutePointsKeepsOriginalOrderWhenRoutePointsMissing() {
        List<ScrapedStation> stations = List.of(
                station(1, "A", 43.857911, 18.424167),
                station(2, "B", 43.857651, 18.419943),
                station(3, "C", 43.859943, 18.411933)
        );

        List<Integer> orderedStationIds = importer.orderStationsByRoutePoints(stations, List.of()).stream()
                .map(ScrapedStation::getId)
                .toList();

        assertEquals(List.of(1, 2, 3), orderedStationIds);
    }

    @Test
    void orderStationsByRoutePointsKeepsStableOrderWhenStationsMapToSameRoutePoint() {
        List<ScrapedStation> stations = List.of(
                station(10, "S1", 43.8600, 18.4100),
                station(11, "S2", 43.8600, 18.4100),
                station(12, "S3", 43.8610, 18.4110)
        );

        List<ScrapedRoutePoint> routePoints = List.of(
                routePoint(1, 43.8600, 18.4100),
                routePoint(2, 43.8610, 18.4110)
        );

        List<Integer> orderedStationIds = importer.orderStationsByRoutePoints(stations, routePoints).stream()
                .map(ScrapedStation::getId)
                .toList();

        assertEquals(List.of(10, 11, 12), orderedStationIds);
    }

    private static ScrapedStation station(int id, String name, double lat, double lon) {
        ScrapedStation station = new ScrapedStation();
        station.setId(id);
        station.setName(name);
        station.setLatitude(BigDecimal.valueOf(lat));
        station.setLongitude(BigDecimal.valueOf(lon));
        return station;
    }

    private static ScrapedRoutePoint routePoint(int id, double lat, double lon) {
        ScrapedRoutePoint routePoint = new ScrapedRoutePoint();
        routePoint.setId(id);
        routePoint.setLatitude(BigDecimal.valueOf(lat));
        routePoint.setLongitude(BigDecimal.valueOf(lon));
        return routePoint;
    }
}
