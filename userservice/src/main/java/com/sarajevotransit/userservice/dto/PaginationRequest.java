package com.sarajevotransit.userservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaginationRequest {

    @Min(value = 0, message = "Page index must be 0 or greater")
    private int page = 0;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size can be at most 100")
    private int size = 20;

    private String sort;
}
