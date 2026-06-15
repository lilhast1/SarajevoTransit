package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.repository.SubscriptionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/subscriptions/admin")
@RequiredArgsConstructor
public class SubscriptionStatsController {

    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/line-stats")
    public List<Map<String, Object>> getLineStats(HttpServletRequest request) {
        if (!"ADMIN".equals(request.getHeader("X-User-Role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return subscriptionRepository.topLinesBySubscriberCount().stream()
                .map(r -> Map.<String, Object>of(
                        "lineId", r[0],
                        "lineCode", r[1] != null ? r[1] : "",
                        "lineName", r[2] != null ? r[2] : "",
                        "subscriberCount", r[3]))
                .toList();
    }
}
