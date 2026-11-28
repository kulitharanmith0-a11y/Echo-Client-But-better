package com.blamejared.clumps.client.utils.strings;

import java.lang.ref.WeakReference;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class Concat implements CharSequence {
    private static final AtomicInteger SEED_GEN = new AtomicInteger(0x51A7C3D9);
    private static final List<WeakReference<Concat>> INSTANCES = new ArrayList<>();

    private final byte[] encoded;
    private final int maskSeed;
    private final int length;

    private Concat(char[] source, int offset, int count) {
        this.length = Math.max(0, count);
        this.maskSeed = mix32(SEED_GEN.addAndGet(0x9E3779B9) ^ (length * 31));
        this.encoded = new byte[this.length << 1];
        for (int i = 0; i < this.length; i++) {
            int ch = source[offset + i];
            int k = charKey(maskSeed, i);
            int masked = ch ^ (k & 0xFFFF);
            int pos = i << 1;
            this.encoded[pos] = (byte) (masked & 0xFF);
            this.encoded[pos + 1] = (byte) ((masked >>> 8) & 0xFF);
        }
        register(this);
    }

    private Concat(CharSequence source, int start, int end) {
        this.length = Math.max(0, end - start);
        this.maskSeed = mix32(SEED_GEN.addAndGet(0x9E3779B9) ^ (length * 31));
        this.encoded = new byte[this.length << 1];
        for (int i = 0; i < this.length; i++) {
            int ch = source.charAt(start + i);
            int k = charKey(maskSeed, i);
            int masked = ch ^ (k & 0xFFFF);
            int pos = i << 1;
            this.encoded[pos] = (byte) (masked & 0xFF);
            this.encoded[pos + 1] = (byte) ((masked >>> 8) & 0xFF);
        }
        register(this);
    }

    private static void register(Concat instance) {
        synchronized (INSTANCES) {
            if ((INSTANCES.size() & 63) == 63) {
                INSTANCES.removeIf(ref -> ref.get() == null);
            }
            INSTANCES.add(new WeakReference<>(instance));
        }
    }

    public static void clearAll() {
        synchronized (INSTANCES) {
            Iterator<WeakReference<Concat>> it = INSTANCES.iterator();
            while (it.hasNext()) {
                Concat c = it.next().get();
                if (c != null) {
                    Arrays.fill(c.encoded, (byte) 0);
                }
                it.remove();
            }
        }
    }

    private static int mix32(int x) {
        x ^= (x >>> 16);
        x *= 0x85EBCA6B;
        x ^= (x >>> 13);
        x *= 0xC2B2AE35;
        x ^= (x >>> 16);
        return x;
    }

    private static int charKey(int seed, int index) {
        return mix32(seed ^ (index * 0x27D4EB2D + 0x165667B1));
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException(index);
        }
        int pos = index << 1;
        int masked = (encoded[pos] & 0xFF) | ((encoded[pos + 1] & 0xFF) << 8);
        return (char) (masked ^ (charKey(maskSeed, index) & 0xFFFF));
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        if (start < 0 || end < start || end > length) {
            throw new IndexOutOfBoundsException();
        }
        return new Concat(this, start, end);
    }

    @Override
    public String toString() {
        if (length == 0) {
            return "";
        }
        char[] out = new char[length];
        for (int i = 0; i < length; i++) {
            out[i] = charAt(i);
        }
        String result = new String(out);
        Arrays.fill(out, '\0');
        return result;
    }

    public static Concat strings(String... parts) {
        if (parts == null || parts.length == 0) {
            return new Concat("", 0, 0);
        }
        int total = 0;
        for (String part : parts) {
            if (part != null) total += part.length();
        }
        char[] buf = new char[total];
        int cursor = 0;
        for (String part : parts) {
            if (part != null) {
                int len = part.length();
                part.getChars(0, len, buf, cursor);
                cursor += len;
            }
        }
        Concat created = new Concat(buf, 0, total);
        Arrays.fill(buf, '\0');
        return created;
    }

    public static Concat sequences(CharSequence... parts) {
        if (parts == null || parts.length == 0) {
            return new Concat("", 0, 0);
        }
        int total = 0;
        for (CharSequence part : parts) {
            if (part != null) total += part.length();
        }
        char[] buf = new char[total];
        int cursor = 0;
        for (CharSequence part : parts) {
            if (part != null) {
                int len = part.length();
                for (int i = 0; i < len; i++) {
                    buf[cursor++] = part.charAt(i);
                }
            }
        }
        Concat created = new Concat(buf, 0, total);
        Arrays.fill(buf, '\0');
        return created;
    }

    public static Concat sequences(CharSequence a, CharSequence b) {
        int lenA = a == null ? 0 : a.length();
        int lenB = b == null ? 0 : b.length();
        char[] buf = new char[lenA + lenB];
        for (int i = 0; i < lenA; i++) buf[i] = a.charAt(i);
        for (int i = 0; i < lenB; i++) buf[lenA + i] = b.charAt(i);
        Concat created = new Concat(buf, 0, buf.length);
        Arrays.fill(buf, '\0');
        return created;
    }

    public static Concat integer(int value) {
        String s = Integer.toString(value);
        return new Concat(s, 0, s.length());
    }

    public static Concat formattedFloat(float value, int decimals) {
        String s = String.format("%." + decimals + "f", value);
        return new Concat(s, 0, s.length());
    }

    public static boolean containsLowercase(CharSequence text, String query) {
        if (text == null || query == null) return false;
        int qLen = query.length();
        if (qLen == 0) return true;
        int tLen = text.length();
        if (qLen > tLen) return false;
        char first = Character.toLowerCase(query.charAt(0));
        int max = tLen - qLen;
        for (int i = 0; i <= max; i++) {
            if (Character.toLowerCase(text.charAt(i)) != first) continue;
            int j = 1;
            while (j < qLen && Character.toLowerCase(text.charAt(i + j)) == Character.toLowerCase(query.charAt(j))) {
                j++;
            }
            if (j == qLen) return true;
        }
        return false;
    }

    public static int compareLowercase(CharSequence a, CharSequence b) {
        if (a == null && b == null) return 0;
        if (a == null) return -1;
        if (b == null) return 1;
        int lenA = a.length();
        int lenB = b.length();
        int min = Math.min(lenA, lenB);
        for (int i = 0; i < min; i++) {
            char ca = Character.toLowerCase(a.charAt(i));
            char cb = Character.toLowerCase(b.charAt(i));
            if (ca != cb) return ca - cb;
        }
        return lenA - lenB;
    }

    public static boolean contentEquals(CharSequence a, CharSequence b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        int len = a.length();
        if (len != b.length()) return false;
        for (int i = 0; i < len; i++) {
            if (a.charAt(i) != b.charAt(i)) return false;
        }
        return true;
    }

    public boolean sameRepresentation(Concat other) {
        return contentEquals(this, other);
    }

    public Concat lowercaseView() {
        char[] buf = new char[length];
        for (int i = 0; i < length; i++) {
            buf[i] = Character.toLowerCase(charAt(i));
        }
        Concat created = new Concat(buf, 0, length);
        Arrays.fill(buf, '\0');
        return created;
    }

    public static String shortHash(CharSequence input) {
        if (input == null) {
            return shortHashBytes(new byte[0]);
        }
        int len = input.length();
        byte[] utf8 = new byte[len * 3];
        int out = 0;
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if (c < 0x80) {
                utf8[out++] = (byte) c;
            } else if (c < 0x800) {
                utf8[out++] = (byte) (0xC0 | (c >> 6));
                utf8[out++] = (byte) (0x80 | (c & 0x3F));
            } else {
                utf8[out++] = (byte) (0xE0 | (c >> 12));
                utf8[out++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                utf8[out++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(utf8, 0, out);
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 4; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            int h = 0;
            for (int i = 0; i < len; i++) h = 31 * h + input.charAt(i);
            return Integer.toHexString(h);
        } finally {
            Arrays.fill(utf8, 0, out, (byte) 0);
        }
    }

    private static String shortHashBytes(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 4; i++) sb.append(String.format("%02x", digest[i]));
            return sb.toString();
        } catch (Exception e) {
            return "00000000";
        }
    }
}
