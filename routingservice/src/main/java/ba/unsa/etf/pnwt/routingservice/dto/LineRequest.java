package ba.unsa.etf.pnwt.routingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LineRequest {

    private Integer externalId;

    @Size(max = 20)
    private String code;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private Short vehicleTypeId;

    @NotNull
    private Boolean isActive;

    public Integer getExternalId() {
        return externalId;
    }

    public void setExternalId(Integer externalId) {
        this.externalId = externalId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Short getVehicleTypeId() {
        return vehicleTypeId;
    }

    public void setVehicleTypeId(Short vehicleTypeId) {
        this.vehicleTypeId = vehicleTypeId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
