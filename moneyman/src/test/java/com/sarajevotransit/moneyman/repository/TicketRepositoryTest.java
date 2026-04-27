package com.sarajevotransit.moneyman.repository;

import com.sarajevotransit.moneyman.model.Ticket;
import com.sarajevotransit.moneyman.model.Transaction;
import com.sarajevotransit.moneyman.model.enums.PaymentStatus;
import com.sarajevotransit.moneyman.model.enums.TicketStatus;
import com.sarajevotransit.moneyman.model.enums.TicketType;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.show-sql=false"
})
public class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void findAllByUserIdWithTransaction_UsesSingleQuery() {
        Transaction transaction = new Transaction();
        transaction.setUserId(1L);
        transaction.setAmount(new BigDecimal("1.80"));
        transaction.setCurrency("BAM");
        transaction.setStatus(PaymentStatus.COMPLETED);
        transaction.setExternalTransactionId("TX-123");
        transaction.setCreatedAt(LocalDateTime.now());
        entityManager.persist(transaction);

        Ticket ticket = new Ticket();
        ticket.setUserId(1L);
        ticket.setType(TicketType.SINGLE);
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setValidFrom(LocalDateTime.now());
        ticket.setValidUntil(LocalDateTime.now().plusHours(1));
        ticket.setQrCodeData("ST-123");
        ticket.setTransaction(transaction);
        entityManager.persist(ticket);
        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        List<Ticket> results = ticketRepository.findAllByUserIdWithTransaction(1L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTransaction()).isNotNull();
        assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
    }
}
