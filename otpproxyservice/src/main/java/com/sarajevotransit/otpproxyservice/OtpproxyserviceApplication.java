package com.sarajevotransit.otpproxyservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OtpproxyserviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OtpproxyserviceApplication.class, args);
    }
}
