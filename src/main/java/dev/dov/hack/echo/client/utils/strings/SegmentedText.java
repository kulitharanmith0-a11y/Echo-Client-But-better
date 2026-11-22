package dev.dov.hack.echo.client.utils.strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public final class SegmentedText implements CharSequence {
    private final String[] segments;
    private final int[] offsets;
    private final int length;

    private SegmentedText(String[] segments) {
        this.segments = segments.clone();
        this.offsets = new int[segments.length];
        int total = 0;
        for (int i = 0; i < segments.length; i++) {
            offsets[i] = total;
            total = Math.addExact(total, Objects.requireNonNull(segments[i], "segment").length());
        }
        this.length = total;
    }

    
    public static SegmentedText literal(String text) {
        Objects.requireNonNull(text, "text");
        return new SegmentedText(text.codePoints().mapToObj(c -> new String(Character.toChars(c))).toArray(String[]::new));
    }

    
    public static SegmentedText ofParts(String... segments) {
        return new SegmentedText(Objects.requireNonNull(segments, "segments"));
    }

    
    @Override public int length() { return length; }

    
    @Override public char charAt(int index) {
        Objects.checkIndex(index, length);
        for (int i = 0; i < segments.length; i++) {
            int end = offsets[i] + segments[i].length();
            if (index < end) return segments[i].charAt(index - offsets[i]);
        }
        throw new AssertionError("Validated index not covered by immutable segments");
    }

    
    @Override public SegmentedText subSequence(int start, int end) {
        Objects.checkFromToIndex(start, end, length);
        ArrayList<String> parts = new ArrayList<>();
        for (int i = 0; i < segments.length; i++) {
            int segmentStart = offsets[i];
            int segmentEnd = segmentStart + segments[i].length();
            if (segmentEnd <= start || segmentStart >= end) continue;
            parts.add(segments[i].substring(Math.max(0, start - segmentStart), Math.min(segments[i].length(), end - segmentStart)));
        }
        return new SegmentedText(parts.toArray(String[]::new));
    }

    
    @Override public String toString() { return String.join("", segments); }

    
    public boolean hasSameSegments(SegmentedText other) {
        return other != null && Arrays.equals(segments, other.segments);
    }
}
