package com.sentrifugo.rms.common.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Tiny helper: IST (Asia/Kolkata) date/time formatting for email bodies throughout RMS.
 * <p>
 * Usage example:
 * <pre>
 *   IstTime.fmtDate(schedule.getInterviewDate())   // "29-Sep-2026"
 *   IstTime.fmtTime(schedule.getStartTime())        // "10:30 AM"
 *   IstTime.today()                                 // LocalDate in IST
 * </pre>
 */
public final class IstTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    private static final DateTimeFormatter DATE_FMT     = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter DATE_LONG_FMT = DateTimeFormatter.ofPattern("d MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT     = DateTimeFormatter.ofPattern("hh:mm a");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy, hh:mm a");

    private IstTime() {}

    /** Current date in IST. */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** Current date-time in IST. */
    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

    /** "29-Sep-2026" — returns "-" for null. */
    public static String fmtDate(LocalDate date) {
        return date != null ? date.format(DATE_FMT) : "-";
    }

    /** Alias used by email builders. */
    public static String formatDate(LocalDate date) {
        return fmtDate(date);
    }

    /** "27 November 2025" — letter-style long date; returns "-" for null. */
    public static String fmtDateLong(LocalDate date) {
        return date != null ? date.format(DATE_LONG_FMT) : "-";
    }

    /** "10:30 AM" — returns "-" for null. */
    public static String fmtTime(LocalTime time) {
        return time != null ? time.format(TIME_FMT) : "-";
    }

    /** Alias used by email builders. */
    public static String formatTime(LocalTime time) {
        return fmtTime(time);
    }

    /** "29-Sep-2026, 10:30 AM IST" — returns "-" for null. */
    public static String fmtDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DATETIME_FMT) + " IST" : "-";
    }

    public static String formatDateTime(LocalDateTime dt) {
        return fmtDateTime(dt);
    }
}
