package com.sarajevotransit.userservice.saga;

import com.sarajevotransit.userservice.config.RabbitMQConfig;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.saga.event.TicketPurchaseInitiatedEvent;
import com.sarajevotransit.userservice.saga.event.TicketRideValidatedEvent;
import com.sarajevotransit.userservice.saga.event.TicketUserFailedEvent;
import com.sarajevotransit.userservice.saga.event.TicketUserUpdatedEvent;
import com.sarajevotransit.userservice.service.LoyaltyService;
import com.sarajevotransit.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSagaListener {

    private static final int LOYALTY_POINTS_SINGLE  = 2;
    private static final int LOYALTY_POINTS_DAILY   = 5;
    private static final int LOYALTY_POINTS_WEEKLY  = 20;
    private static final int LOYALTY_POINTS_MONTHLY = 50;
    private static final int LOYALTY_POINTS_RIDE_VALIDATION = 1;

    private final UserService userService;
    private final LoyaltyService loyaltyService;
    private final TicketSagaPublisher publisher;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_PURCHASE_INITIATED)
    public void onTicketPurchaseInitiated(TicketPurchaseInitiatedEvent event) {
        log.info("Saga [{}]: received TicketPurchaseInitiated for user {}", event.sagaId(), event.userId());
        try {
            int points = resolvePoints(event.ticketType());
            // Single atomic transaction: both TicketPurchaseHistory and LoyaltyPoints are written or neither is
            userService.recordTicketPurchaseSaga(
                    event.userId(),
                    event.ticketType(),
                    event.amount(),
                    event.externalTransactionId(),
                    points
            );
            log.info("Saga [{}]: user records written — publishing UserUpdated", event.sagaId());
            publisher.publishUserUpdated(new TicketUserUpdatedEvent(event.sagaId(), points));
        } catch (Exception e) {
            // The @Transactional in recordTicketPurchaseSaga rolled back both writes — nothing to undo here
            log.error("Saga [{}]: user record write failed — publishing UserFailed. Cause: {}", event.sagaId(), e.getMessage());
            publisher.publishUserFailed(new TicketUserFailedEvent(event.sagaId(), e.getMessage()));
        }
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_USER_RIDE_VALIDATED)
    public void onRideValidated(TicketRideValidatedEvent event) {
        log.info("Ride validation received for user {} and ticket {}", event.userId(), event.ticketId());
        loyaltyService.earnPoints(
                event.userId(),
                new LoyaltyEarnRequest(
                        Math.max(event.points(), LOYALTY_POINTS_RIDE_VALIDATION),
                        "Ride validated for ticket " + event.ticketType(),
                        "RIDE_VALIDATION"
                )
        );
    }

    private int resolvePoints(String ticketType) {
        return switch (ticketType) {
            case "DAILY"   -> LOYALTY_POINTS_DAILY;
            case "WEEKLY"  -> LOYALTY_POINTS_WEEKLY;
            case "MONTHLY" -> LOYALTY_POINTS_MONTHLY;
            default        -> LOYALTY_POINTS_SINGLE;  // SINGLE
        };
    }
}
