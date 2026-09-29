package com.sentrifugo.rms.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

/** Force JVM default timezone to IST so bare LocalDateTime.now() matches Asia/Kolkata on Azure too. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class IstTimezoneBootstrap {

    @PostConstruct
    public void applyIstDefault() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
    }
}
