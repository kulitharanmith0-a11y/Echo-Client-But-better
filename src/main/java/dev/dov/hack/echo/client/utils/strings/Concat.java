package dev.dov.hack.echo.client.utils.strings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Concat implements CharSequence {
    private final String value;

    private Concat(String value) {
        this.value = value;
    }

    @Override
    public int length() {
        return value.length();
    }

    @Override
    public char charAt(int index) {
        return value.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return new Concat(value.substring(start, end));
    }

    @Override
    public String toString() {
        return value;
    }

    public static Concat strings(String... parts) {
        if (parts == null || parts.length == 0) return new Concat("");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) if (part != null) sb.append(part);
        return new Concat(sb.toString());
    }

    public static Concat sequences(CharSequence... parts) {
        if (parts == null || parts.length == 0) return new Concat("");
        StringBuilder sb = new StringBuilder();
        for (CharSequence part : parts) if (part != null) sb.append(part);
        return new Concat(sb.toString());
    }

    public static Concat sequences(CharSequence a, CharSequence b) {
        return new Concat((a != null ? a.toString() : "") + (b != null ? b.toString() : ""));
    }

    public static Concat integer(int value) {
        return new Concat(Integer.toString(value));
    }

    public static Concat formattedFloat(float value, int decimals) {
        return new Concat(String.format("%." + decimals + "f", value));
    }

    public static boolean containsLowercase(CharSequence text, String query) {
        if (text == null || query == null) return false;
        return text.toString().toLowerCase().contains(query.toLowerCase());
    }

    public static int compareLowercase(CharSequence a, CharSequence b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        return a.toString().toLowerCase().compareTo(b.toString().toLowerCase());
    }

    public static boolean contentEquals(CharSequence a, CharSequence b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.toString().equals(b.toString());
    }

    public boolean sameRepresentation(Concat other) {
        return other != null && value.equals(other.value);
    }

    public Concat lowercaseView() {
        return new Concat(value.toLowerCase());
    }

    public static String shortHash(CharSequence input) {
        return shortHash(input != null ? input.toString() : "");
    }

    private static String shortHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }
}
