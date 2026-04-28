package com.sarajevotransit.vehicleservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sarajevotransit.vehicleservice.model.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

}