package dev.garado.transit.gtfs

/**
 * One dataset entry from Transitous's config.yml 
 * https://api.transitous.org/gtfs/config.yml
 */
data class GtfsDataset(
    /** e.g. "at-PTA-Styria-Flex-2026" */
    val key: String,
    /** ISO 3166-1 alpha-2 country/region code, e.g. "at", "au-nsw" */
    val regionCode: String,
    /** Filename under https://api.transitous.org/gtfs/, e.g. "at_PTA-Styria-Flex-2026.gtfs.zip" */
    val path: String,
    /** From the directory index; null if it couldn't be matched up. */
    val sizeBytes: Long? = null,
) {
    val downloadUrl: String get() = "$TRANSITOUS_GTFS_BASE_URL/$path"
}

fun List<GtfsDataset>.totalSizeBytes(): Long = sumOf { it.sizeBytes ?: 0L }

fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    bytes >= 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

const val TRANSITOUS_GTFS_BASE_URL = "https://api.transitous.org/gtfs"
