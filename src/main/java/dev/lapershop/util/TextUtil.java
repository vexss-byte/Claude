package dev.lapershop.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class TextUtil {
    private TextUtil() {}

    /** Teks dengan gradasi warna per huruf. */
    public static MutableComponent gradient(String text, int start, int end, boolean bold) {
        MutableComponent out = Component.empty();
        int[] cols = Gradient.colors(start, end, text.length());
        for (int i = 0; i < text.length(); i++) {
            Style st = Style.EMPTY.withColor(TextColor.fromRgb(cols[i])).withBold(bold);
            out.append(Component.literal(String.valueOf(text.charAt(i))).setStyle(st));
        }
        return out;
    }

    /** Warna vanilla untuk kode '0'-'9','a'-'f' (RGB). -1 jika bukan kode warna. */
    private static int colorOf(char c) {
        return switch (c) {
            case '0' -> 0x000000; case '1' -> 0x0000AA; case '2' -> 0x00AA00; case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000; case '5' -> 0xAA00AA; case '6' -> 0xFFAA00; case '7' -> 0xAAAAAA;
            case '8' -> 0x555555; case '9' -> 0x5555FF; case 'a' -> 0x55FF55; case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555; case 'd' -> 0xFF55FF; case 'e' -> 0xFFFF55; case 'f' -> 0xFFFFFF;
            default -> -1;
        };
    }

    /**
     * Parser kode warna gaya '&' (&a, &l, &#RRGGBB, &r). Italic dimatikan secara default.
     * Tidak memakai ChatFormatting (isi kelas itu dikurangi di Minecraft 26.2+); hanya Style.
     */
    public static MutableComponent legacy(String in) {
        MutableComponent out = Component.empty();
        if (in == null) return out;
        Style base = Style.EMPTY.withItalic(false);
        Style style = base;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '&' && i + 1 < in.length()) {
                char n = Character.toLowerCase(in.charAt(i + 1));
                if (n == '#' && i + 7 < in.length()) {
                    try {
                        int rgb = Integer.parseInt(in.substring(i + 2, i + 8), 16);
                        flush(out, buf, style);
                        style = style.withColor(TextColor.fromRgb(rgb));
                        i += 7;
                        continue;
                    } catch (NumberFormatException ignored) { }
                }
                int color = colorOf(n);
                if (color >= 0) { // warna: reset format lain, seperti perilaku vanilla
                    flush(out, buf, style);
                    style = base.withColor(TextColor.fromRgb(color));
                    i++;
                    continue;
                }
                Style next = switch (n) {
                    case 'l' -> style.withBold(true);
                    case 'o' -> style.withItalic(true);
                    case 'n' -> style.withUnderlined(true);
                    case 'm' -> style.withStrikethrough(true);
                    case 'k' -> style.withObfuscated(true);
                    case 'r' -> base;
                    default -> null;
                };
                if (next != null) {
                    flush(out, buf, style);
                    style = next;
                    i++;
                    continue;
                }
            }
            buf.append(c);
        }
        flush(out, buf, style);
        return out;
    }

    private static void flush(MutableComponent out, StringBuilder buf, Style style) {
        if (buf.length() > 0) {
            out.append(Component.literal(buf.toString()).setStyle(style));
            buf.setLength(0);
        }
    }
}
