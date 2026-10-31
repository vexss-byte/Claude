package dev.lapershop.util;

import net.minecraft.ChatFormatting;
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

    /** Parser kode warna gaya '&' (&a, &l, &#RRGGBB, &r). Italic dimatikan secara default. */
    public static MutableComponent legacy(String in) {
        MutableComponent out = Component.empty();
        if (in == null) return out;
        Style base = Style.EMPTY.withItalic(false);
        Style style = base;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < in.length(); i++) {
            char c = in.charAt(i);
            if (c == '&' && i + 1 < in.length()) {
                char n = in.charAt(i + 1);
                if (n == '#' && i + 7 < in.length()) {
                    try {
                        int rgb = Integer.parseInt(in.substring(i + 2, i + 8), 16);
                        flush(out, buf, style);
                        style = style.withColor(TextColor.fromRgb(rgb));
                        i += 7;
                        continue;
                    } catch (NumberFormatException ignored) { }
                }
                ChatFormatting f = ChatFormatting.getByCode(Character.toLowerCase(n));
                if (f != null) {
                    flush(out, buf, style);
                    if (f == ChatFormatting.RESET) style = base;
                    else if (f.isColor()) style = base.applyFormat(f);
                    else style = style.applyFormat(f);
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
