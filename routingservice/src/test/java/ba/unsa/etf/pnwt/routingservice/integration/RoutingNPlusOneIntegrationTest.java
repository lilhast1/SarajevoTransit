package ba.unsa.etf.pnwt.routingservice.integration;

import ba.unsa.etf.pnwt.routingservice.model.Line;
import ba.unsa.etf.pnwt.routingservice.model.VehicleType;
import ba.unsa.etf.pnwt.routingservice.repository.LineRepository;
import ba.unsa.etf.pnwt.routingservice.repository.VehicleTypeRepository;
import ba.unsa.etf.pnwt.routingservice.service.RoutingCrudService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "routing.import.enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
class RoutingNPlusOneIntegrationTest {

    @Autowired
    private RoutingCrudService routingCrudService;

    @Autowired
    private VehicleTypeRepository vehicleTypeRepository;

    @Autowired
    private LineRepository lineRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private EntityManager entityManager;

    @Test
    void getLinesDoesNotTriggerNPlusOneQueries() {
        VehicleType vehicleType = vehicleTypeRepository.findById((short) 2)
                .orElseGet(() -> {
                    VehicleType created = new VehicleType();
                    created.setId((short) 2);
                    created.setName("bus");
                    return vehicleTypeRepository.save(created);
                });

        for (int i = 0; i < 3; i++) {
            Line line = new Line();
            line.setExternalId(900000 + i);
            line.setCode("T" + i);
            line.setName("Test line " + i);
            line.setVehicleType(vehicleType);
            line.setIsActive(true);
            lineRepository.save(line);
        }

        entityManager.flush();
        entityManager.clear();

        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics statistics = sessionFactory.getStatistics();
        statistics.clear();

        List<?> result = routingCrudService.getLines(true, null);

        long queryCount = statistics.getPrepareStatementCount();
        assertFalse(result.isEmpty());
        assertTrue(queryCount <= 2, "Expected no N+1. Query count=" + queryCount);
    }
}
