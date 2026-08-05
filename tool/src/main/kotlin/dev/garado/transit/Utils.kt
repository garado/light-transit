package dev.garado.transit

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CLOCK_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a")

/** Duration formatting, e.g. "35m" "1h13m" */
fun formatDuration(durationSeconds: Long): String {
    val totalMinutes = durationSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h${minutes.toString().padStart(2, '0')}m" else "${minutes}m"
}

/** e.g. "3:45 PM" */
fun formatClockTime(epochSeconds: Long): String =
    CLOCK_TIME_FORMAT.format(Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()))

/** e.g. "3:45 PM - 4:58 PM" */
fun formatTimeRange(startTimeSeconds: Long, endTimeSeconds: Long): String =
    "${formatClockTime(startTimeSeconds)} - ${formatClockTime(endTimeSeconds)}"
