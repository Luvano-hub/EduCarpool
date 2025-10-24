package com.educarpool;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

public final class TimeUtils {

    private TimeUtils() {}

    /** Parse ISO-8601 with timezone (e.g., "2025-10-23T10:15:30.123456+02:00") to epoch millis. */
    public static long parseIsoOffsetToEpochMillis(String iso) {
        if (iso == null || iso.isEmpty()) return 0L;
        try {
            return OffsetDateTime.parse(iso).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {

            try {
                String trimmed = iso.replaceFirst("(\\.\\d{3})\\d+(?=[+-]\\d\\d:\\d\\d|Z)", "$1");
                return OffsetDateTime.parse(trimmed).toInstant().toEpochMilli();
            } catch (Exception ignored) {
                return 0L;
            }
        }
    }
}

