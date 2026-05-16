package com.sarajevotransit.moneyman.saga;

import com.sarajevotransit.moneyman.config.RabbitMQConfig;
import com.sarajevotransit.moneyman.model.Transaction;
import com.sarajevotransit.moneyman.model.enums.PaymentStatus;
import com.sarajevotransit.moneyman.model.enums.TicketStatus;
import com.sarajevotransit.moneyman.repository.TransactionRepository;
import com.sarajevotransit.moneyman.saga.event.TicketPurchaseCancelledEvent;
import com.sarajevotransit.moneyman.saga.event.TicketPurchaseCompletedEvent;
import com.sarajevotransit.moneyman.saga.event.TicketUserFailedEvent;
import com.sarajevotransit.moneyman.saga.event.TicketUserUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketSagaListener {

    private final TransactionRepository transactionRepository;
    private final TicketSagaPublisher publisher;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_MONEYMAN_USER_UPDATED)
    @Transactional
    public void onTicketUserUpdated(TicketUserUpdatedEvent event) {
        log.info("Saga [{}]: UserService succeeded — finalizing ticket", event.sagaId());
        transactionRepository.findBySagaIdWithTicket(event.sagaId()).ifPresentOrElse(tx -> {
            tx.setStatus(PaymentStatus.COMPLETED);
            tx.getTicket().setStatus(TicketStatus.ACTIVE);
            transactionRepository.save(tx);
            log.info("Saga [{}]: Transaction COMPLETED, Ticket ACTIVE — saga FINALIZED", event.sagaId());
            publisher.publishPurchaseCompleted(new TicketPurchaseCompletedEvent(
                    event.sagaId(),
                    tx.getUserId(),
                    tx.getTicket().getId(),
                    tx.getTicket().getType().name()
            ));
        }, () -> log.error("Saga [{}]: Transaction not found for finalization", event.sagaId()));
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_MONEYMAN_USER_FAILED)
    @Transactional
    public void onTicketUserFailed(TicketUserFailedEvent event) {
        log.warn("Saga [{}]: UserService failed — compensating. Reason: {}", event.sagaId(), event.reason());
        transactionRepository.findBySagaIdWithTicket(event.sagaId()).ifPresentOrElse(tx -> {
            tx.setStatus(PaymentStatus.FAILED);
            tx.getTicket().setStatus(TicketStatus.CANCELLED);
            transactionRepository.save(tx);
            log.info("Saga [{}]: Transaction FAILED, Ticket CANCELLED — compensation COMPLETE", event.sagaId());
            publisher.publishPurchaseCancelled(new TicketPurchaseCancelledEvent(
                    event.sagaId(),
                    tx.getUserId(),
                    event.reason()
            ));
        }, () -> log.error("Saga [{}]: Transaction not found for compensation", event.sagaId()));
    }
}
