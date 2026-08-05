package dev.garado.transit

/** Duration formatting, e.g. "35m" "1h13m" */
fun formatDuration(durationSeconds: Long): String {
    val totalMinutes = durationSeconds / 60
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h${minutes.toString().padStart(2, '0')}m" else "${minutes}m"
}
