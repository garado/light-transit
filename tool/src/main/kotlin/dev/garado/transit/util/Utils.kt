package dev.garado.transit.util

import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val CLOCK_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a")

/** Duration formatting, e.g. "35m" "1h13m" */
fun formatDuration(durationSeconds: Long): String {
    val totalMinutes = durationSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h${minutes.toString().padStart(2, '0')}m" else "${minutes}m"
}

/** Duration split for stacked display, e.g. ["35m"] or ["1h", "11m"] */
fun formatDurationLines(durationSeconds: Long): List<String> {
    val totalMinutes = durationSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) listOf("${hours}h", "${minutes}m") else listOf("${minutes}m")
}

/** e.g. "3:45 PM" */
fun formatClockTime(epochSeconds: Long): String =
    CLOCK_TIME_FORMAT.format(Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()))

/** e.g. "3:45 PM" from a 24h hour + minute pair */
fun formatClockTime(hour24: Int, minute: Int): String {
    val isPm = hour24 >= 12
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return "$hour12:${minute.toString().padStart(2, '0')} ${if (isPm) "PM" else "AM"}"
}

/** The next occurrence (today, or tomorrow if that clock time has already passed) of [hour24]:[minute]. */
fun nextEpochSecondsFor(hour24: Int, minute: Int): Long {
    val now = ZonedDateTime.now()
    var candidate = now.withHour(hour24).withMinute(minute).withSecond(0).withNano(0)
    if (candidate.isBefore(now)) candidate = candidate.plusDays(1)
    return candidate.toEpochSecond()
}

/** e.g. "3:45 PM - 4:58 PM" */
fun formatTimeRange(startTimeSeconds: Long, endTimeSeconds: Long): String =
    "${formatClockTime(startTimeSeconds)} - ${formatClockTime(endTimeSeconds)}"

/** parses GTFS-style hex color string (no leading '#') into a [Color], or [fallback] if missing/invalid */
fun parseHexColor(hex: String?, fallback: Color): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        Color(AndroidColor.parseColor("#$hex"))
    } catch (e: IllegalArgumentException) {
        fallback
    }
}
