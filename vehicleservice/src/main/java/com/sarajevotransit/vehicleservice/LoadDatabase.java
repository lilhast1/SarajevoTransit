package com.sarajevotransit.vehicleservice;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sarajevotransit.vehicleservice.model.DriverProfile;
import com.sarajevotransit.vehicleservice.model.Vehicle;
import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleType;
import com.sarajevotransit.vehicleservice.repository.DriverProfileRepository;
import com.sarajevotransit.vehicleservice.repository.VehicleRepository;

@Configuration
public class LoadDatabase {
    private static final Logger logger = LoggerFactory.getLogger(LoadDatabase.class);

    // Sarajevo GRAS fleet seed — covers all active lines with 20% buffer
    // Trams: 6 lines × ~13 avg → 78 trams
    // Trolleybuses: 7 lines × ~9 avg → 63 trolleys
    // Buses: 42 lines × ~5 avg → 210 buses
    // Minibuses: 12 lines × ~3 avg → 36 minibuses
    // Total: ~387 vehicles

    private static final int TRAM_COUNT     = 78;
    private static final int TROLLEY_COUNT  = 63;
    private static final int BUS_COUNT      = 210;
    private static final int MINIBUS_COUNT  = 36;

    @Bean
    CommandLineRunner initDatabase(VehicleRepository vehicleRepository,
                                   DriverProfileRepository driverProfileRepository) {
        return args -> {
            if (vehicleRepository.count() > 0) {
                logger.info("Vehicles already seeded, skipping");
                return;
            }
            logger.info("Seeding GRAS fleet...");
            Random rng = new Random(42);
            List<Vehicle> vehicles = new ArrayList<>();

            // Trams — Sarajevo tram fleet uses Düwag and CAF Urbos 3 cars
            for (int i = 1; i <= TRAM_COUNT; i++) {
                vehicles.add(makeVehicle(
                        String.format("A%02d-T-%03d", rng.nextInt(99) + 1, i),
                        String.valueOf(300 + i),
                        VehicleType.TRAM, 200,
                        randomDate(rng, 2000, 2022)));
            }

            // Trolleybuses — Hess/Solaris trolleys
            for (int i = 1; i <= TROLLEY_COUNT; i++) {
                vehicles.add(makeVehicle(
                        String.format("B%02d-TR-%03d", rng.nextInt(99) + 1, i),
                        String.valueOf(500 + i),
                        VehicleType.TROLLEY, 80,
                        randomDate(rng, 2005, 2022)));
            }

            // Buses — mixed fleet (standard city buses)
            for (int i = 1; i <= BUS_COUNT; i++) {
                vehicles.add(makeVehicle(
                        String.format("C%02d-B-%03d", rng.nextInt(99) + 1, i),
                        String.valueOf(100 + i),
                        VehicleType.BUS, 70,
                        randomDate(rng, 2003, 2023)));
            }

            // Minibuses
            for (int i = 1; i <= MINIBUS_COUNT; i++) {
                vehicles.add(makeVehicle(
                        String.format("D%02d-M-%03d", rng.nextInt(99) + 1, i),
                        String.valueOf(700 + i),
                        VehicleType.MINIBUS, 22,
                        randomDate(rng, 2010, 2023)));
            }

            vehicleRepository.saveAll(vehicles);
            logger.info("Seeded {} vehicles ({} trams, {} trolleys, {} buses, {} minibuses)",
                    vehicles.size(), TRAM_COUNT, TROLLEY_COUNT, BUS_COUNT, MINIBUS_COUNT);

            // Seed driver profiles — userIds 4-8 match DataSeeder in userservice
            // (admin=1, amina=2, tarik=3, then 5 driver users)
            Object[][] driverData = {
                {"Emir Hodzic",  4L, "+38761100001", "BA-D-001001", List.of(VehicleType.TRAM, VehicleType.BUS)},
                {"Selma Muric",  5L, "+38761100002", "BA-D-001002", List.of(VehicleType.TROLLEY, VehicleType.BUS)},
                {"Damir Begic",  6L, "+38761100003", "BA-D-001003", List.of(VehicleType.BUS, VehicleType.MINIBUS)},
                {"Lejla Avdic",  7L, "+38761100004", "BA-D-001004", List.of(VehicleType.TRAM, VehicleType.TROLLEY)},
                {"Nedim Konjic", 8L, "+38761100005", "BA-D-001005", List.of(VehicleType.BUS)},
            };
            List<DriverProfile> profiles = new ArrayList<>();
            for (Object[] d : driverData) {
                DriverProfile dp = new DriverProfile();
                dp.setFullName((String) d[0]);
                dp.setUserId((Long) d[1]);
                dp.setPhone((String) d[2]);
                dp.setLicenseNumber((String) d[3]);
                @SuppressWarnings("unchecked")
                List<VehicleType> types = (List<VehicleType>) d[4];
                dp.setLicensedVehicleTypes(types);
                dp.setCreatedAt(LocalDateTime.now());
                profiles.add(dp);
            }
            driverProfileRepository.saveAll(profiles);
            logger.info("Seeded {} driver profiles", profiles.size());
        };
    }

    private static Vehicle makeVehicle(String reg, String internalId,
                                        VehicleType type, int capacity, LocalDate mfgDate) {
        Vehicle v = new Vehicle();
        v.setRegistrationNumber(reg);
        v.setInternalId(internalId);
        v.setType(type);
        v.setCapacity(capacity);
        v.setManufactureDate(mfgDate);
        v.setStatus(VehicleStatus.OPERATIONAL);
        return v;
    }

    private static LocalDate randomDate(Random rng, int fromYear, int toYear) {
        int year = fromYear + rng.nextInt(toYear - fromYear + 1);
        int month = rng.nextInt(12) + 1;
        int day = rng.nextInt(28) + 1;
        return LocalDate.of(year, month, day);
    }
}
