package ba.unsa.etf.pnwt.routingservice.dto;

import java.math.BigDecimal;
import java.util.List;

public class GeoJsonGeometryResponse {

    private String type;
    private List<List<BigDecimal>> coordinates;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<List<BigDecimal>> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<List<BigDecimal>> coordinates) {
        this.coordinates = coordinates;
    }
}
