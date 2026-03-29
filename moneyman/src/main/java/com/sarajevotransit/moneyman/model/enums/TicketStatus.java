package com.sarajevotransit.moneyman.model.enums;

public enum TicketStatus {
    ACTIVE,      // Valid for use
    USED,        // Single ticket that was scanned/validated
    EXPIRED,     // Past its validity date
    CANCELLED    // Refunded
}


