package ba.unsa.etf.pnwt.routingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "active_delays")
@IdClass(ActiveDelay.PK.class)
public class ActiveDelay {

    @Id
    @Column(name = "timetable_id")
    private Integer timetableId;

    @Id
    @Column(name = "service_date", length = 8)
    private String serviceDate;

    @Column(name = "direction_id")
    private Integer directionId;

    @Column(name = "delay_seconds")
    private Integer delaySeconds;

    @Column(name = "reason", length = 300)
    private String reason;

    @Column(name = "line_id")
    private Long lineId;

    @Column(name = "line_code", length = 20)
    private String lineCode;

    @Column(name = "line_name", length = 200)
    private String lineName;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public ActiveDelay() {}

    public ActiveDelay(Integer timetableId, String serviceDate, Integer directionId,
                       Integer delaySeconds, String reason,
                       Long lineId, String lineCode, String lineName, Instant updatedAt) {
        this.timetableId = timetableId;
        this.serviceDate = serviceDate;
        this.directionId = directionId;
        this.delaySeconds = delaySeconds;
        this.reason = reason;
        this.lineId = lineId;
        this.lineCode = lineCode;
        this.lineName = lineName;
        this.updatedAt = updatedAt;
    }

    public Integer getTimetableId() { return timetableId; }
    public String getServiceDate()  { return serviceDate; }
    public Integer getDirectionId() { return directionId; }
    public Integer getDelaySeconds(){ return delaySeconds; }
    public String getReason()       { return reason; }
    public Long getLineId()         { return lineId; }
    public String getLineCode()     { return lineCode; }
    public String getLineName()     { return lineName; }
    public Instant getUpdatedAt()   { return updatedAt; }

    public void setDelaySeconds(Integer delaySeconds) { this.delaySeconds = delaySeconds; }
    public void setReason(String reason)              { this.reason = reason; }
    public void setUpdatedAt(Instant updatedAt)       { this.updatedAt = updatedAt; }

    public static class PK implements Serializable {
        private Integer timetableId;
        private String serviceDate;
        public PK() {}
        public PK(Integer timetableId, String serviceDate) {
            this.timetableId = timetableId;
            this.serviceDate = serviceDate;
        }
        @Override public boolean equals(Object o) {
            if (!(o instanceof PK pk)) return false;
            return java.util.Objects.equals(timetableId, pk.timetableId)
                && java.util.Objects.equals(serviceDate, pk.serviceDate);
        }
        @Override public int hashCode() {
            return java.util.Objects.hash(timetableId, serviceDate);
        }
    }
}
