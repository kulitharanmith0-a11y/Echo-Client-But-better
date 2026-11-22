package dev.jadyen.hack.echo.natives;

import java.util.Objects;

public final class OfflineSession {
    private static final OfflineSession SHARED = new OfflineSession();

    private static final String PROFILE_LIST_FAILURE =
            "{\"success\":false,\"error\":\"Cloud service unavailable offline\",\"activeConfigId\":\"\",\"configs\":[]}";
    private static final String PROFILE_FAILURE =
            "{\"success\":false,\"error\":\"Cloud service unavailable offline\",\"activeConfigId\":\"\",\"config\":null,\"data\":\"\"}";
    private static final String PROFILE_DELETE_FAILURE =
            "{\"success\":false,\"error\":\"Cloud service unavailable offline\",\"deletedConfigId\":\"\",\"activeConfigId\":\"\"}";

    private volatile boolean initialized;

    
    public static OfflineSession shared() {
        return SHARED;
    }

    
    public void markInitialized() {
        initialized = true;
    }

    
    public String call(String operation, String[] arguments) {
        Objects.requireNonNull(operation, "operation");
        if (arguments != null) {
            for (int index = 0; index < arguments.length; index++) {
                Objects.requireNonNull(arguments[index], "arguments[" + index + "]");
            }
        }
        return switch (operation) {
            case "uid" -> initialized ? "0" : "";
            case "is_available" -> "true";
            case "list_profiles_json" -> PROFILE_LIST_FAILURE;
            case "get_profile_json", "save_profile_json", "select_profile_json" -> PROFILE_FAILURE;
            case "delete_profile_json" -> PROFILE_DELETE_FAILURE;
            default -> "";
        };
    }
}
