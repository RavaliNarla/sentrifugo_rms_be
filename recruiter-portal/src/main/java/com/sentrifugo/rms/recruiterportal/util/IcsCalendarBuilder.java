package com.sentrifugo.rms.recruiterportal.util;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Minimal RFC 5545 calendar files (METHOD:PUBLISH) for Outlook / Gmail / Apple Calendar.
 * PUBLISH = add as a reminder; RSVP is handled by our Accept / Decline links, not the calendar UI.
 */
public final class IcsCalendarBuilder {

    public static final String CONTENT_TYPE = "text/calendar";
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");
    /** Split into multiple .ics files when a day has more events than this. */
    public static final int MAX_EVENTS_PER_FILE = 25;

    private static final DateTimeFormatter ICS_UTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter ICS_LOCAL = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private IcsCalendarBuilder() {
    }

    public record Event(
            String uid,
            String summary,
            String description,
            String location,
            LocalDate date,
            LocalTime start,
            LocalTime end
    ) {
    }

    public record NamedIcs(String fileName, byte[] bytes) {
    }

    public static byte[] singleEventIcs(Event event) {
        return buildCalendar(List.of(event)).getBytes(StandardCharsets.UTF_8);
    }

    /** One or more .ics attachments covering all events (split if large). */
    public static List<NamedIcs> multiEventIcsFiles(String baseFileName, List<Event> events) {
        if (events == null || events.isEmpty()) {
            return List.of();
        }
        String base = baseFileName == null || baseFileName.isBlank() ? "interviews" : baseFileName;
        if (!base.toLowerCase().endsWith(".ics")) {
            base = base + ".ics";
        }
        String stem = base.substring(0, base.length() - 4);

        List<NamedIcs> files = new ArrayList<>();
        int parts = (events.size() + MAX_EVENTS_PER_FILE - 1) / MAX_EVENTS_PER_FILE;
        for (int p = 0; p < parts; p++) {
            int from = p * MAX_EVENTS_PER_FILE;
            int to = Math.min(events.size(), from + MAX_EVENTS_PER_FILE);
            List<Event> chunk = events.subList(from, to);
            String name = parts == 1 ? stem + ".ics" : stem + "-part" + (p + 1) + ".ics";
            files.add(new NamedIcs(name, buildCalendar(chunk).getBytes(StandardCharsets.UTF_8)));
        }
        return files;
    }

    public static String buildCalendar(List<Event> events) {
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("PRODID:-//Sentrifugo RMS//Interview Invite//EN\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("BEGIN:VTIMEZONE\r\n");
        sb.append("TZID:").append(DEFAULT_ZONE.getId()).append("\r\n");
        sb.append("BEGIN:STANDARD\r\n");
        sb.append("DTSTART:19700101T000000\r\n");
        sb.append("TZOFFSETFROM:+0530\r\n");
        sb.append("TZOFFSETTO:+0530\r\n");
        sb.append("TZNAME:IST\r\n");
        sb.append("END:STANDARD\r\n");
        sb.append("END:VTIMEZONE\r\n");

        String now = ZonedDateTime.now(ZoneId.of("UTC")).format(ICS_UTC);
        for (Event event : events) {
            if (event == null || event.date() == null || event.start() == null || event.end() == null) {
                continue;
            }
            String uid = event.uid() != null ? event.uid() : UUID.randomUUID() + "@sentrifugo-rms";
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("UID:").append(escapeText(uid)).append("\r\n");
            sb.append("DTSTAMP:").append(now).append("\r\n");
            sb.append("DTSTART;TZID=").append(DEFAULT_ZONE.getId()).append(":")
                    .append(LocalDateTime.of(event.date(), event.start()).format(ICS_LOCAL)).append("\r\n");
            sb.append("DTEND;TZID=").append(DEFAULT_ZONE.getId()).append(":")
                    .append(LocalDateTime.of(event.date(), event.end()).format(ICS_LOCAL)).append("\r\n");
            sb.append("SUMMARY:").append(escapeText(nullToEmpty(event.summary()))).append("\r\n");
            if (event.description() != null && !event.description().isBlank()) {
                sb.append("DESCRIPTION:").append(escapeText(event.description())).append("\r\n");
            }
            if (event.location() != null && !event.location().isBlank()) {
                sb.append("LOCATION:").append(escapeText(event.location())).append("\r\n");
            }
            sb.append("STATUS:CONFIRMED\r\n");
            sb.append("TRANSP:OPAQUE\r\n");
            sb.append("END:VEVENT\r\n");
        }
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String escapeText(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n");
    }
}
