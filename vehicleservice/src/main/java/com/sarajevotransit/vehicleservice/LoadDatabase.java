package com.sarajevotransit.vehicleservice;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sarajevotransit.vehicleservice.model.Vehicle;
import com.sarajevotransit.vehicleservice.model.enums.VehicleStatus;
import com.sarajevotransit.vehicleservice.model.enums.VehicleType;
import com.sarajevotransit.vehicleservice.repository.VehicleRepository;

@Configuration
public class LoadDatabase {
    private static final Logger logger = LoggerFactory.getLogger(LoadDatabase.class);

    @Bean
    CommandLineRunner initDatabase(VehicleRepository vehicleRepository) {
        Vehicle v = new Vehicle(
                "E67-0-143",
                "tramvaj 3 T3",
                VehicleType.TRAM,
                200,
                LocalDate.now(),
                VehicleStatus.OPERATIONAL,
                41.22,
                43.22,
                LocalDateTime.now());
        return args -> {
            logger.info("Preloading vehicle");
            vehicleRepository.save(v);
        };
    }
}
