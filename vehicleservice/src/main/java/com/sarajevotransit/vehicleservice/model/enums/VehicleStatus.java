package com.sarajevotransit.vehicleservice.model.enums;

public enum VehicleStatus {
    OPERATIONAL, // Ready for the road
    IN_MAINTENANCE, // Currently in the garage for scheduled check
    OUT_OF_SERVICE, // Broken down / Unplanned repair
    RETIRED // No longer in use (history only)
}
