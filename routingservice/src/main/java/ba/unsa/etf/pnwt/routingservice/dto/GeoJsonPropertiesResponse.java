package ba.unsa.etf.pnwt.routingservice.dto;

public class GeoJsonPropertiesResponse {

    private Integer directionId;
    private Integer directionExternalId;
    private String directionName;
    private Integer lineId;
    private String lineName;
    private Integer pointCount;

    public Integer getDirectionId() {
        return directionId;
    }

    public void setDirectionId(Integer directionId) {
        this.directionId = directionId;
    }

    public Integer getDirectionExternalId() {
        return directionExternalId;
    }

    public void setDirectionExternalId(Integer directionExternalId) {
        this.directionExternalId = directionExternalId;
    }

    public String getDirectionName() {
        return directionName;
    }

    public void setDirectionName(String directionName) {
        this.directionName = directionName;
    }

    public Integer getLineId() {
        return lineId;
    }

    public void setLineId(Integer lineId) {
        this.lineId = lineId;
    }

    public String getLineName() {
        return lineName;
    }

    public void setLineName(String lineName) {
        this.lineName = lineName;
    }

    public Integer getPointCount() {
        return pointCount;
    }

    public void setPointCount(Integer pointCount) {
        this.pointCount = pointCount;
    }
}
