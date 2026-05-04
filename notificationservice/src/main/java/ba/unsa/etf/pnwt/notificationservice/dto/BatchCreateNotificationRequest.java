package ba.unsa.etf.pnwt.notificationservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchCreateNotificationRequest {

    @NotEmpty
    @Size(max = 100, message = "Batch size must not exceed 100")
    @Valid
    private List<CreateNotificationRequest> notifications;
}
