package com.blamejared.clumps.client.diagnostics;

public final class DevDiagnostics {
    public static final boolean ENABLED = false;

    private DevDiagnostics() {
    }

    public static void install() {
    }

    public static void log(String source, String event, String detail) {
    }

    public static void logRateLimited(String key, long minIntervalMs, String source, String event, String detail) {
    }

    public static void metric(String metricKey) {
    }
}
