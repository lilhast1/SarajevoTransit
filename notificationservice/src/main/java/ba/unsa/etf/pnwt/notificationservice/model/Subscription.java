package ba.unsa.etf.pnwt.notificationservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_full_name", length = 255)
    private String userFullName;

    @Email
    @Column(name = "user_email", length = 255)
    private String userEmail;

    @NotNull
    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @Column(name = "line_code", length = 20)
    private String lineCode;

    @Column(name = "line_name", length = 200)
    private String lineName;

    @Column(name = "start_interval")
    private LocalTime startInterval;

    @Column(name = "end_interval")
    private LocalTime endInterval;

    @Column(name = "days_of_week", length = 50)
    private String daysOfWeek;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
