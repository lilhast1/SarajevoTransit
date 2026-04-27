package ba.unsa.etf.pnwt.routingservice.dto;

public class GeoJsonFeatureResponse {

    private String type;
    private GeoJsonGeometryResponse geometry;
    private GeoJsonPropertiesResponse properties;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public GeoJsonGeometryResponse getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJsonGeometryResponse geometry) {
        this.geometry = geometry;
    }

    public GeoJsonPropertiesResponse getProperties() {
        return properties;
    }

    public void setProperties(GeoJsonPropertiesResponse properties) {
        this.properties = properties;
    }
}
