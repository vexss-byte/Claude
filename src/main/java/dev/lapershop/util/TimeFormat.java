package dev.lapershop.util;

public final class TimeFormat {
    private TimeFormat() {}

    /** 7199 -> "01:59:59". */
    public static String hms(long totalSeconds) {
        long s = Math.max(0, totalSeconds);
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }
}
