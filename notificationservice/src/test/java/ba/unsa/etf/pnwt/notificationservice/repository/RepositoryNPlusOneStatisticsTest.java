package ba.unsa.etf.pnwt.notificationservice.repository;

import ba.unsa.etf.pnwt.notificationservice.model.Notification;
import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import ba.unsa.etf.pnwt.notificationservice.model.Subscription;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class RepositoryNPlusOneStatisticsTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        subscriptionRepository.deleteAll();
        seedNotifications();
        seedSubscriptions();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void notification_findAll_executesSingleSelect_withoutNPlusOne() {
        Statistics statistics = statistics();
        statistics.clear();

        notificationRepository.findAll().forEach(n -> n.getTitle());

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    void notification_findByUserId_executesSingleSelect_withoutNPlusOne() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Statistics statistics = statistics();
        statistics.clear();

        notificationRepository.findByUserId(userId).forEach(n -> n.getTitle());

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    void notification_findByUserIdAndIsRead_executesSingleSelect_withoutNPlusOne() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Statistics statistics = statistics();
        statistics.clear();

        notificationRepository.findByUserIdAndIsRead(userId, false).forEach(n -> n.getTitle());

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    void subscription_findAll_executesSingleSelect_withoutNPlusOne() {
        Statistics statistics = statistics();
        statistics.clear();

        subscriptionRepository.findAll().forEach(s -> s.getLineName());

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    void subscription_findByUserId_executesSingleSelect_withoutNPlusOne() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Statistics statistics = statistics();
        statistics.clear();

        subscriptionRepository.findByUserId(userId).forEach(s -> s.getLineName());

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    void subscription_findByLineId_executesSingleSelect_withoutNPlusOne() {
        UUID lineId = UUID.fromString("00000000-0000-0000-0000-000000000101");
        Statistics statistics = statistics();
        statistics.clear();

        subscriptionRepository.findByLineId(lineId).forEach(s -> s.getLineName());

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    void subscription_findByUserIdAndIsActive_executesSingleSelect_withoutNPlusOne() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Statistics statistics = statistics();
        statistics.clear();

        subscriptionRepository.findByUserIdAndIsActive(userId, true).forEach(s -> s.getLineName());

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    void subscription_findByUserFullNameContainingIgnoreCase_executesSingleSelect_withoutNPlusOne() {
        Statistics statistics = statistics();
        statistics.clear();

        subscriptionRepository.findByUserFullNameContainingIgnoreCase("ana").forEach(s -> s.getLineName());

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    @Test
    void subscription_findByUserEmailIgnoreCase_executesSingleSelect_withoutNPlusOne() {
        Statistics statistics = statistics();
        statistics.clear();

        subscriptionRepository.findByUserEmailIgnoreCase("ana.aganovic@example.com").forEach(s -> s.getLineName());

        assertEquals(1, statistics.getPrepareStatementCount());
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private void seedNotifications() {
        Notification n1 = new Notification();
        n1.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        n1.setUserFullName("Ana Aganovic");
        n1.setUserEmail("ana.aganovic@example.com");
        n1.setLineId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
        n1.setLineCode("3");
        n1.setLineName("Ilidza - Bascarsija");
        n1.setType(NotificationType.DELAY);
        n1.setTitle("Kasnjenje");
        n1.setContent("Kasnjenje 10 min");
        n1.setIsRead(false);
        n1.setSentAt(LocalDateTime.now().minusMinutes(10));

        Notification n2 = new Notification();
        n2.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        n2.setUserFullName("Ana Aganovic");
        n2.setUserEmail("ana.aganovic@example.com");
        n2.setLineId(UUID.fromString("00000000-0000-0000-0000-000000000103"));
        n2.setLineCode("31");
        n2.setLineName("Skenderija - Dobrinja");
        n2.setType(NotificationType.UPCOMING_DEPARTURE);
        n2.setTitle("Polazak");
        n2.setContent("Polazak za 5 minuta");
        n2.setIsRead(false);
        n2.setSentAt(LocalDateTime.now().minusMinutes(5));

        Notification n3 = new Notification();
        n3.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        n3.setUserFullName("Mirza Hodzic");
        n3.setUserEmail("mirza.hodzic@example.com");
        n3.setLineId(UUID.fromString("00000000-0000-0000-0000-000000000102"));
        n3.setLineCode("1");
        n3.setLineName("Ilidza - Centar");
        n3.setType(NotificationType.ROUTE_CHANGE);
        n3.setTitle("Izmjena trase");
        n3.setContent("Privremena izmjena trase");
        n3.setIsRead(true);
        n3.setSentAt(LocalDateTime.now().minusHours(1));

        notificationRepository.save(n1);
        notificationRepository.save(n2);
        notificationRepository.save(n3);
    }

    private void seedSubscriptions() {
        Subscription s1 = new Subscription();
        s1.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        s1.setUserFullName("Ana Aganovic");
        s1.setUserEmail("ana.aganovic@example.com");
        s1.setLineId(UUID.fromString("00000000-0000-0000-0000-000000000101"));
        s1.setLineCode("3");
        s1.setLineName("Ilidza - Bascarsija");
        s1.setStartInterval(LocalTime.of(7, 0));
        s1.setEndInterval(LocalTime.of(9, 0));
        s1.setDaysOfWeek("MON,TUE,WED");
        s1.setIsActive(true);

        Subscription s2 = new Subscription();
        s2.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        s2.setUserFullName("Ana Aganovic");
        s2.setUserEmail("ana.aganovic@example.com");
        s2.setLineId(UUID.fromString("00000000-0000-0000-0000-000000000103"));
        s2.setLineCode("31");
        s2.setLineName("Skenderija - Dobrinja");
        s2.setStartInterval(LocalTime.of(15, 0));
        s2.setEndInterval(LocalTime.of(17, 0));
        s2.setDaysOfWeek("MON,TUE,WED");
        s2.setIsActive(false);

        Subscription s3 = new Subscription();
        s3.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        s3.setUserFullName("Mirza Hodzic");
        s3.setUserEmail("mirza.hodzic@example.com");
        s3.setLineId(UUID.fromString("00000000-0000-0000-0000-000000000102"));
        s3.setLineCode("1");
        s3.setLineName("Ilidza - Centar");
        s3.setStartInterval(LocalTime.of(8, 0));
        s3.setEndInterval(LocalTime.of(10, 0));
        s3.setDaysOfWeek("MON,FRI");
        s3.setIsActive(true);

        subscriptionRepository.save(s1);
        subscriptionRepository.save(s2);
        subscriptionRepository.save(s3);
    }
}
