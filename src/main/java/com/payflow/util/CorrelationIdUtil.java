package com.payflow.util;

import org.slf4j.MDC;

public class CorrelationIdUtil {

    public static String getCorrelationId() {
        return MDC.get("correlationId");
    }
}