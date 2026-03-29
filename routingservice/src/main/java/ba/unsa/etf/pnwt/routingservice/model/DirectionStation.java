package ba.unsa.etf.pnwt.routingservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "direction_stations", indexes = {
        @Index(name = "idx_dir_stations_direction", columnList = "direction_id"),
        @Index(name = "idx_dir_stations_station", columnList = "station_id")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"direction_id", "stop_sequence"}),
        @UniqueConstraint(columnNames = {"direction_id", "station_id"})
})
public class DirectionStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "direction_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Direction direction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "stop_sequence", nullable = false)
    private Integer stopSequence;

    @Column(name = "travel_time_from_prev_seconds")
    private Integer travelTimeFromPrevSeconds;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public Station getStation() {
        return station;
    }

    public void setStation(Station station) {
        this.station = station;
    }

    public Integer getStopSequence() {
        return stopSequence;
    }

    public void setStopSequence(Integer stopSequence) {
        this.stopSequence = stopSequence;
    }

    public Integer getTravelTimeFromPrevSeconds() {
        return travelTimeFromPrevSeconds;
    }

    public void setTravelTimeFromPrevSeconds(Integer travelTimeFromPrevSeconds) {
        this.travelTimeFromPrevSeconds = travelTimeFromPrevSeconds;
    }
}
