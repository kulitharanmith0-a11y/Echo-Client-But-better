package com.blamejared.clumps.client;

import com.blamejared.clumps.client.init.ClientBootstrap;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class PreLaunch implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        installLogFilter();
        ClientBootstrap.prepare();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void installLogFilter() {
        try {
            Class<?> logManagerClass = Class.forName("org.apache.logging.log4j.LogManager");
            Method getContext = logManagerClass.getMethod("getContext", boolean.class);
            Object rawContext = getContext.invoke(null, Boolean.FALSE);

            Class<?> coreContextClass = Class.forName("org.apache.logging.log4j.core.LoggerContext");
            if (!coreContextClass.isInstance(rawContext)) {
                return;
            }

            Class<?> filterClass = Class.forName("org.apache.logging.log4j.core.Filter");
            Class<?> resultClass = Class.forName("org.apache.logging.log4j.core.Filter$Result");
            Class<?> logEventClass = Class.forName("org.apache.logging.log4j.core.LogEvent");
            Class<?> messageClass = Class.forName("org.apache.logging.log4j.message.Message");
            Class<?> lifeCycleStateClass = Class.forName("org.apache.logging.log4j.core.LifeCycle$State");

            Object deny = Enum.valueOf((Class<Enum>) resultClass, "DENY");
            Object neutral = Enum.valueOf((Class<Enum>) resultClass, "NEUTRAL");
            Object started = Enum.valueOf((Class<Enum>) lifeCycleStateClass, "STARTED");

            InvocationHandler handler = (proxy, method, args) -> {
                String name = method.getName();
                if ("hashCode".equals(name)) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(name)) {
                    return args != null && args.length == 1 && proxy == args[0];
                }
                if ("toString".equals(name)) {
                    return "DefaultFilter";
                }
                if ("getOnMatch".equals(name) || "getOnMismatch".equals(name)) {
                    return neutral;
                }
                if ("getState".equals(name)) {
                    return started;
                }
                if ("isStarted".equals(name)) {
                    return Boolean.TRUE;
                }
                if ("isStopped".equals(name)) {
                    return Boolean.FALSE;
                }
                if ("initialize".equals(name) || "start".equals(name) || "stop".equals(name)) {
                    return null;
                }
                if ("filter".equals(name) && args != null) {
                    if (args.length == 1 && logEventClass.isInstance(args[0])) {
                        Object event = args[0];
                        String loggerName = invokeString(event, "getLoggerName");
                        Object message = invokeZero(event, "getMessage");
                        String formatted = message == null ? null : invokeString(message, "getFormattedMessage");
                        Throwable thrown = (Throwable) invokeZero(event, "getThrown");
                        Throwable msgThrown = message == null ? null : (Throwable) invokeZero(message, "getThrowable");
                        if (shouldSuppressLogger(loggerName)
                                || containsSuppressedToken(formatted)
                                || containsSuppressedThrowable(thrown)
                                || containsSuppressedThrowable(msgThrown)) {
                            return deny;
                        }
                        return neutral;
                    }
                    if (args.length >= 4) {
                        String loggerName = args[0] == null ? null : invokeString(args[0], "getName");
                        if (shouldSuppressLogger(loggerName) || containsSuppressedArgs(args, 3, messageClass)) {
                            return deny;
                        }
                    }
                    return neutral;
                }
                return null;
            };

            Object filter = Proxy.newProxyInstance(
                    coreContextClass.getClassLoader(),
                    new Class<?>[] {filterClass},
                    handler
            );

            Method addContextFilter = coreContextClass.getMethod("addFilter", filterClass);
            addContextFilter.invoke(rawContext, filter);

            Object config = coreContextClass.getMethod("getConfiguration").invoke(rawContext);
            if (config != null) {
                Class<?> filterableClass = Class.forName("org.apache.logging.log4j.core.filter.Filterable");
                filterableClass.getMethod("addFilter", filterClass).invoke(config, filter);
            }
            coreContextClass.getMethod("updateLoggers").invoke(rawContext);
        } catch (Throwable ignored) {
        }
    }

    private static boolean containsSuppressedArgs(Object[] args, int start, Class<?> messageClass) {
        for (int i = start; i < args.length; i++) {
            if (containsSuppressedObject(args[i], messageClass)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsSuppressedObject(Object value, Class<?> messageClass) {
        if (value == null) {
            return false;
        }
        if (value instanceof Throwable thrown) {
            return containsSuppressedThrowable(thrown);
        }
        if (messageClass.isInstance(value)) {
            if (containsSuppressedToken(invokeString(value, "getFormattedMessage"))) {
                return true;
            }
            if (containsSuppressedThrowable((Throwable) invokeZero(value, "getThrowable"))) {
                return true;
            }
            Object[] params = (Object[]) invokeZero(value, "getParameters");
            if (params != null) {
                for (Object param : params) {
                    if (containsSuppressedObject(param, messageClass)) {
                        return true;
                    }
                }
            }
            return false;
        }
        if (value instanceof Object[] array) {
            for (Object item : array) {
                if (containsSuppressedObject(item, messageClass)) {
                    return true;
                }
            }
            return false;
        }
        return containsSuppressedToken(String.valueOf(value));
    }

    private static boolean containsSuppressedThrowable(Throwable thrown) {
        Throwable current = thrown;
        int depth = 0;
        while (current != null && depth++ < 16) {
            if (containsSuppressedToken(current.getClass().getName()) || containsSuppressedToken(current.getMessage())) {
                return true;
            }
            for (StackTraceElement frame : current.getStackTrace()) {
                if (containsSuppressedToken(frame.getClassName()) || containsSuppressedToken(frame.getFileName())) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean shouldSuppressLogger(String loggerName) {
        if (loggerName == null || loggerName.isEmpty()) {
            return false;
        }
        String lower = loggerName.toLowerCase(Locale.ROOT);
        return "clumps".equals(lower) || containsSuppressedToken(lower);
    }

    private static boolean containsSuppressedToken(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("clumps")
                || lower.contains("com.blamejared.clumps")
                || lower.contains("com/blamejared/clumps");
    }

    private static Object invokeZero(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String invokeString(Object target, String methodName) {
        Object result = invokeZero(target, methodName);
        return result instanceof String s ? s : null;
    }
}
