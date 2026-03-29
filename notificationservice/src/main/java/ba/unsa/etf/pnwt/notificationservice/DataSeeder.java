package ba.unsa.etf.pnwt.notificationservice;

import ba.unsa.etf.pnwt.notificationservice.model.Notification;
import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import ba.unsa.etf.pnwt.notificationservice.model.Subscription;
import ba.unsa.etf.pnwt.notificationservice.repository.NotificationRepository;
import ba.unsa.etf.pnwt.notificationservice.repository.SubscriptionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Component
public class DataSeeder implements CommandLineRunner {

    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;

    public DataSeeder(NotificationRepository notificationRepository,
                      SubscriptionRepository subscriptionRepository) {
        this.notificationRepository = notificationRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        // Seed subscriptions
        if (subscriptionRepository.count() == 0) {
            Subscription s1 = new Subscription();
            s1.setUserId(1L);
            s1.setLineId(3L);
            s1.setStartInterval(LocalTime.of(7, 0));
            s1.setEndInterval(LocalTime.of(9, 0));
            s1.setDaysOfWeek("MON,TUE,WED,THU,FRI");
            s1.setIsActive(true);

            Subscription s2 = new Subscription();
            s2.setUserId(2L);
            s2.setLineId(1L);
            s2.setStartInterval(LocalTime.of(8, 0));
            s2.setEndInterval(LocalTime.of(10, 0));
            s2.setDaysOfWeek("MON,WED,FRI");
            s2.setIsActive(true);

            Subscription s3 = new Subscription();
            s3.setUserId(1L);
            s3.setLineId(31L);
            s3.setStartInterval(LocalTime.of(15, 0));
            s3.setEndInterval(LocalTime.of(17, 0));
            s3.setDaysOfWeek("MON,TUE,WED,THU,FRI");
            s3.setIsActive(false);

            subscriptionRepository.save(s1);
            subscriptionRepository.save(s2);
            subscriptionRepository.save(s3);

            System.out.println(">>> Subscriptions seeded.");
        }

        // Seed notifications
        if (notificationRepository.count() == 0) {
            Notification n1 = new Notification();
            n1.setUserId(1L);
            n1.setLineId(3L);
            n1.setType(NotificationType.DELAY);
            n1.setTitle("Kašnjenje na liniji 3");
            n1.setContent("Tramvaj na liniji 3 kasni approximately 10 minuta zbog saobraćajne gužve kod Vijećnice.");
            n1.setIsRead(false);
            n1.setSentAt(LocalDateTime.now().minusMinutes(15));

            Notification n2 = new Notification();
            n2.setUserId(2L);
            n2.setLineId(1L);
            n2.setType(NotificationType.ROUTE_CHANGE);
            n2.setTitle("Izmjena trase linije 1");
            n2.setContent("Zbog radova na Otoci, linija 1 privremeno ne saobraća do stanice Ilidža.");
            n2.setIsRead(true);
            n2.setSentAt(LocalDateTime.now().minusHours(2));

            Notification n3 = new Notification();
            n3.setUserId(1L);
            n3.setLineId(31L);
            n3.setType(NotificationType.UPCOMING_DEPARTURE);
            n3.setTitle("Polazak za 5 minuta");
            n3.setContent("Linija 31e polazi sa stanice Skenderija za 5 minuta.");
            n3.setIsRead(false);
            n3.setSentAt(LocalDateTime.now().minusMinutes(5));

            Notification n4 = new Notification();
            n4.setUserId(3L);
            n4.setLineId(null);
            n4.setType(NotificationType.GENERAL);
            n4.setTitle("Obavještenje o novom redu vožnje");
            n4.setContent("Od 1. aprila stupa na snagu novi ljetni red vožnje za sve linije.");
            n4.setIsRead(false);
            n4.setSentAt(LocalDateTime.now());

            notificationRepository.save(n1);
            notificationRepository.save(n2);
            notificationRepository.save(n3);
            notificationRepository.save(n4);

            System.out.println(">>> Notifications seeded.");
        }
    }
}