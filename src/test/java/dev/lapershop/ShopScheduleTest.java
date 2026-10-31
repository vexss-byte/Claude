package dev.lapershop;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.Test;

import dev.lapershop.shop.ShopSchedule;
import dev.lapershop.util.Gradient;
import dev.lapershop.util.TimeFormat;

class ShopScheduleTest {
    private static final ZoneId Z = ZoneId.of("Asia/Jakarta");
    private static final List<LocalTime> TIMES = List.of(LocalTime.of(12, 0), LocalTime.of(20, 0));

    private static Instant at(int y, int m, int d, int h, int min) {
        return LocalDateTime.of(y, m, d, h, min).atZone(Z).toInstant();
    }

    @Test void openDuringWindow() {
        var w = ShopSchedule.active(at(2026, 11, 1, 12, 30), Z, TIMES, 120);
        assertNotNull(w);
        assertEquals(7200, w.durationSeconds());
        assertEquals(5400, w.remainingSeconds(at(2026, 11, 1, 12, 30)));
    }

    @Test void closedBetweenWindows() {
        assertNull(ShopSchedule.active(at(2026, 11, 1, 15, 0), Z, TIMES, 120));
        assertNull(ShopSchedule.active(at(2026, 11, 1, 11, 59), Z, TIMES, 120));
    }

    @Test void endIsExclusive() {
        assertNull(ShopSchedule.active(at(2026, 11, 1, 14, 0), Z, TIMES, 120));
    }

    @Test void windowCrossingMidnight() {
        var w = ShopSchedule.active(at(2026, 11, 2, 0, 30), Z, List.of(LocalTime.of(23, 0)), 120);
        assertNotNull(w);
    }

    @Test void durationClampedToTwoHours() {
        var w = ShopSchedule.active(at(2026, 11, 1, 12, 0), Z, TIMES, 999);
        assertNotNull(w);
        assertEquals(7200, w.durationSeconds());
        assertNull(ShopSchedule.active(at(2026, 11, 1, 14, 30), Z, TIMES, 999));
    }

    @Test void nextOpening() {
        var n = ShopSchedule.next(at(2026, 11, 1, 15, 0), Z, TIMES);
        assertEquals(LocalTime.of(20, 0), n.toLocalTime());
        var tomorrow = ShopSchedule.next(at(2026, 11, 1, 21, 0), Z, TIMES);
        assertEquals(LocalTime.of(12, 0), tomorrow.toLocalTime());
        assertEquals(2, tomorrow.getDayOfMonth());
    }

    @Test void parseTimesIgnoresInvalid() {
        assertEquals(2, ShopSchedule.parseTimes(List.of("12:00", "bad", "20:30")).size());
    }

    @Test void timeFormat() {
        assertEquals("01:59:59", TimeFormat.hms(7199));
        assertEquals("00:00:00", TimeFormat.hms(-5));
    }

    @Test void gradientEndpoints() {
        int[] c = Gradient.colors(0xFFFF55, 0xFFAA00, 5);
        assertEquals(0xFFFF55, c[0]);
        assertEquals(0xFFAA00, c[4]);
        assertEquals(0xFFAA00, Gradient.parseHex("#FFAA00", 0));
        assertEquals(7, Gradient.parseHex("zz", 7));
    }
}
