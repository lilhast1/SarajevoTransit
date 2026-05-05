package com.sarajevotransit.otpproxyservice.dto;

import java.util.List;

public record OtpGraphQlResponse(Data data) {

    public record Data(List<Stop> stops) {
    }

    public record Stop(String id) {
    }
}
