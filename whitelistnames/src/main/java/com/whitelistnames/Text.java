package com.whitelistnames;

import net.minecraft.network.chat.Component;

import java.util.regex.Pattern;

/** Small helpers for &-color codes and {placeholder} messages. */
public final class Text {
    private static final Pattern AMP_CODE = Pattern.compile("&([0-9a-fk-orA-FK-OR])");
    private static final Pattern ANY_CODE = Pattern.compile("[&\u00A7][0-9a-fk-orA-FK-OR]");

    private Text() {}

    public static String colorize(String s) {
        return AMP_CODE.matcher(s).replaceAll("\u00A7$1");
    }

    public static String strip(String s) {
        return ANY_CODE.matcher(s).replaceAll("");
    }

    public static Component of(String s) {
        return Component.literal(colorize(s));
    }

    /** format("Hi {name}", "name", "Bob") */
    public static Component format(String template, String... keyValues) {
        String out = template;
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            out = out.replace("{" + keyValues[i] + "}", keyValues[i + 1]);
        }
        return of(out);
    }
}
