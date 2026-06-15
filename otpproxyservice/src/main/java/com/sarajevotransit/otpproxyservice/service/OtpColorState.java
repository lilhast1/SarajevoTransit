package com.sarajevotransit.otpproxyservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class OtpColorState {

    private static final Logger LOGGER = LoggerFactory.getLogger(OtpColorState.class);

    private final AtomicReference<String> activeColor;
    private final AtomicReference<Boolean> rebuilding = new AtomicReference<>(false);

    @Value("${otp.blue.url}")
    private String blueUrl;

    @Value("${otp.green.url}")
    private String greenUrl;

    public OtpColorState(@Value("${otp.active-color}") String initialColor) {
        this.activeColor = new AtomicReference<>(initialColor.toUpperCase());
        LOGGER.info("OTP color state initialized to {}", this.activeColor.get());
    }

    public String getActiveColor() {
        return activeColor.get();
    }

    public String getInactiveColor() {
        return "BLUE".equals(activeColor.get()) ? "GREEN" : "BLUE";
    }

    public String getActiveUrl() {
        return "BLUE".equals(activeColor.get()) ? blueUrl : greenUrl;
    }

    public String getInactiveUrl() {
        return "BLUE".equals(activeColor.get()) ? greenUrl : blueUrl;
    }

    public void switchTo(String color) {
        String prev = activeColor.getAndSet(color.toUpperCase());
        LOGGER.info("OTP traffic switched from {} to {}", prev, color.toUpperCase());
    }

    public boolean isRebuilding() {
        return rebuilding.get();
    }

    public void setRebuilding(boolean value) {
        rebuilding.set(value);
    }

    public String getBlueUrl() {
        return blueUrl;
    }

    public String getGreenUrl() {
        return greenUrl;
    }
}
