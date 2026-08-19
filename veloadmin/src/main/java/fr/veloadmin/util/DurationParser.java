package fr.veloadmin.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses durations like "1d2h30m", "45m", "3h", "7d" into milliseconds.
 * Supported units: w (weeks), d (days), h (hours), m (minutes), s (seconds).
 */
public final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile("(\\d+)([wdhms])");

    private DurationParser() {}

    /** @return duration in milliseconds, or -1 if the string could not be parsed / was empty of matches */
    public static long parseToMillis(String input) {
        if (input == null || input.isBlank()) return -1;
        Matcher matcher = PATTERN.matcher(input.toLowerCase());
        long totalMillis = 0;
        boolean matchedAny = false;

        while (matcher.find()) {
            matchedAny = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            totalMillis += switch (unit) {
                case "w" -> value * 7L * 24 * 60 * 60 * 1000;
                case "d" -> value * 24L * 60 * 60 * 1000;
                case "h" -> value * 60L * 60 * 1000;
                case "m" -> value * 60L * 1000;
                case "s" -> value * 1000L;
                default -> 0L;
            };
        }
        return matchedAny ? totalMillis : -1;
    }

    public static String humanize(long millis) {
        long seconds = millis / 1000;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("j ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
