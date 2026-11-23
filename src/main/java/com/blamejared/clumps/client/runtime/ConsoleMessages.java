package com.blamejared.clumps.client.runtime;

import org.mozilla.javascript.Context;
import org.slf4j.Logger;

public final class ConsoleMessages {
    private ConsoleMessages() {
    }

    public static void info(Logger logger, Object[] arguments) {
    }

    public static void warn(Logger logger, Object[] arguments) {
    }

    public static void error(Logger logger, Object[] arguments) {
    }

    public static void debug(Logger logger, Object[] arguments) {
    }

    public static String format(Object[] arguments) {
        if (arguments.length == 0) {
            return "";
        }
        StringBuilder message = new StringBuilder();
        for (int index = 0; index < arguments.length; index++) {
            if (index > 0) {
                message.append(' ');
            }
            message.append(Context.toString(arguments[index]));
        }
        return message.toString();
    }
}
