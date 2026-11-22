package dev.dov.hack.echo.client.rendering.impl.opengl.utils;

public final class ShaderUtil {
    private ShaderUtil() {
    }

    public static String withHeader(String source, String version) {
        String body = source == null ? "" : source;
        return "#version " + (version == null ? "150" : version) + "\n" + body;
    }

    public static boolean hasErrors(String log) {
        return log != null && (log.contains("ERROR") || log.contains("error:"));
    }

    
    public static java.util.List<Integer> errorLines(String log) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        if (log == null) {
            return out;
        }
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("(\\d+):(\\d+)").matcher(log);
        while (matcher.find()) {
            out.add(Integer.parseInt(matcher.group(2)));
        }
        java.util.Collections.sort(out);
        return out;
    }

    public static String summarize(String log, int maxChars) {
        if (log == null || log.isEmpty()) {
            return "ok";
        }
        String firstLine = log.split("\n")[0];
        return firstLine.length() <= maxChars ? firstLine : firstLine.substring(0, maxChars);
    }
}
