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

    public static void recordEventDispatch(String moduleName, String eventName, long durationNanos, boolean cancelledEvent) {
    }

    public static void recordCrash(String moduleName, String eventName, String methodName, Throwable error) {
    }

    public static void recordDeny(String moduleName, String category, String reason) {
    }

    public static void recordModuleState(String moduleName, String stateEvent, String detail) {
    }

    public static void recordSettingChange(com.blamejared.clumps.client.settings.Setting setting) {
    }
}
