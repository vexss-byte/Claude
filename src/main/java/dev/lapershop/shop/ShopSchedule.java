package dev.lapershop.shop;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Logika jadwal event berbasis waktu nyata (murni Java, bisa diuji). Tahan restart karena dihitung dari jam. */
public final class ShopSchedule {
    public static final int MAX_MINUTES = 120;

    private ShopSchedule() {}

    public record Window(long startEpochSec, long endEpochSec) {
        public long remainingSeconds(Instant now) { return Math.max(0, endEpochSec - now.getEpochSecond()); }
        public long durationSeconds() { return endEpochSec - startEpochSec; }
    }

    public static int clampDuration(int minutes) { return Math.max(1, Math.min(MAX_MINUTES, minutes)); }

    public static List<LocalTime> parseTimes(Collection<String> raw) {
        List<LocalTime> out = new ArrayList<>();
        if (raw == null) return out;
        for (String s : raw) {
            try { out.add(LocalTime.parse(s.trim())); } catch (Exception ignored) { }
        }
        return out;
    }

    /** Window event yang sedang berlangsung pada {@code now}, atau null jika toko tutup. */
    public static Window active(Instant now, ZoneId zone, List<LocalTime> starts, int durationMinutes) {
        int dur = clampDuration(durationMinutes);
        ZonedDateTime nowZ = now.atZone(zone);
        LocalDate today = nowZ.toLocalDate();
        Window best = null;
        for (int d = -1; d <= 0; d++) { // kemarin dicek agar event lewat tengah malam tetap terdeteksi
            for (LocalTime t : starts) {
                ZonedDateTime s = ZonedDateTime.of(today.plusDays(d), t, zone);
                ZonedDateTime e = s.plusMinutes(dur);
                if (!nowZ.isBefore(s) && nowZ.isBefore(e)) {
                    if (best == null || s.toEpochSecond() > best.startEpochSec()) {
                        best = new Window(s.toEpochSecond(), e.toEpochSecond());
                    }
                }
            }
        }
        return best;
    }

    /** Jadwal buka berikutnya setelah {@code now}, atau null jika tidak ada jadwal. */
    public static ZonedDateTime next(Instant now, ZoneId zone, List<LocalTime> starts) {
        LocalDate today = now.atZone(zone).toLocalDate();
        ZonedDateTime best = null;
        for (int d = 0; d <= 1; d++) {
            for (LocalTime t : starts) {
                ZonedDateTime s = ZonedDateTime.of(today.plusDays(d), t, zone);
                if (s.toInstant().isAfter(now) && (best == null || s.isBefore(best))) best = s;
            }
        }
        return best;
    }
}
