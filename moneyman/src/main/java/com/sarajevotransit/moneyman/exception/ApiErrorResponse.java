package com.sarajevotransit.moneyman.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiErrorResponse {
    private String error;
    private String message;
    private Map<String, String> fieldErrors;
}
