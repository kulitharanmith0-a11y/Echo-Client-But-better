package dev.dov.hack.echo.client.event;

public enum EventPhase {
    NONE, EARLY, PRE, POST;

    private static boolean dispatchState;

    static {
        if (!getDispatchState()) {
            setDispatchState(true);
        }
    }

    
    public static void setDispatchState(boolean value) {
        dispatchState = value;
    }

    public static boolean getDispatchState() {
        return dispatchState;
    }

    public static boolean isDispatchStateClear() {
        return !getDispatchState();
    }
}
