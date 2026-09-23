package com.kaisabiyyistudio.tarkana_android.telemetry;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrashReporterTest {

    @Test
    public void testSanitizeBearerToken() {
        String log = "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.secretpayload.signature in request";
        String sanitized = CrashReporter.sanitize(log);

        assertFalse(sanitized.contains("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.secretpayload.signature"));
        assertTrue(sanitized.contains("Bearer [REDACTED]"));
    }

    @Test
    public void testSanitizePassword() {
        String log = "Payload: {\"email\":\"user@example.com\",\"password\":\"SuperSecret123!\"}";
        String sanitized = CrashReporter.sanitize(log);

        assertFalse(sanitized.contains("SuperSecret123!"));
        assertTrue(sanitized.contains("\"password\":\"[REDACTED]\""));
    }

    @Test
    public void testSanitizeHexHashTokens() {
        String log = "Failed operation for guest hash: a3f1c29e47d8b5a034981e7f62bcde890123456789abcdef0123456789abcdef";
        String sanitized = CrashReporter.sanitize(log);

        assertFalse(sanitized.contains("a3f1c29e47d8b5a034981e7f62bcde890123456789abcdef0123456789abcdef"));
        assertTrue(sanitized.contains("[HASH_REDACTED]"));
    }

    @Test
    public void testPreserveInnocentLogMessage() {
        String log = "NullPointerException at com.kaisabiyyistudio.tarkana_android.SessionActivity.renderQuestion(SessionActivity.java:42)";
        String sanitized = CrashReporter.sanitize(log);

        assertEquals(log, sanitized);
    }
}
