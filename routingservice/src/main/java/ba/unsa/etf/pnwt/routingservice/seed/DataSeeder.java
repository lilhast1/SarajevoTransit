package ba.unsa.etf.pnwt.routingservice.seed;

import ba.unsa.etf.pnwt.routingservice.model.VehicleType;
import ba.unsa.etf.pnwt.routingservice.repository.VehicleTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(1)
public class DataSeeder implements CommandLineRunner {

    private final VehicleTypeRepository vehicleTypeRepository;

    public DataSeeder(VehicleTypeRepository vehicleTypeRepository) {
        this.vehicleTypeRepository = vehicleTypeRepository;
    }

    @Override
    public void run(String... args) {
        if (vehicleTypeRepository.count() > 0) {
            return;
        }

        vehicleTypeRepository.saveAll(List.of(
                create((short) 1, "minibus"),
                create((short) 2, "bus"),
                create((short) 3, "trolleybus"),
                create((short) 4, "tram")
        ));
    }

    private VehicleType create(short id, String name) {
        VehicleType vehicleType = new VehicleType();
        vehicleType.setId(id);
        vehicleType.setName(name);
        return vehicleType;
    }
}
