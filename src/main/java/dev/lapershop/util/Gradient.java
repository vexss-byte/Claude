package dev.lapershop.util;

/** Utilitas gradasi warna (tanpa dependensi Minecraft agar bisa diuji). */
public final class Gradient {
    private Gradient() {}

    public static int lerp(int start, int end, float t) {
        int r = Math.round(((start >> 16) & 0xFF) + (((end >> 16) & 0xFF) - ((start >> 16) & 0xFF)) * t);
        int g = Math.round(((start >> 8) & 0xFF) + (((end >> 8) & 0xFF) - ((start >> 8) & 0xFF)) * t);
        int b = Math.round((start & 0xFF) + ((end & 0xFF) - (start & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }

    public static int[] colors(int start, int end, int n) {
        if (n <= 0) return new int[0];
        int[] out = new int[n];
        if (n == 1) { out[0] = start; return out; }
        for (int i = 0; i < n; i++) out[i] = lerp(start, end, i / (float) (n - 1));
        return out;
    }

    /** Menerima "#RRGGBB" atau "RRGGBB". */
    public static int parseHex(String s, int fallback) {
        if (s == null) return fallback;
        String h = s.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.length() != 6) return fallback;
        try { return Integer.parseInt(h, 16); } catch (NumberFormatException e) { return fallback; }
    }
}
