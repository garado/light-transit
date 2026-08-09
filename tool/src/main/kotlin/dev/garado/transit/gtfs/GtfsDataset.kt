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
) {
    val downloadUrl: String get() = "$TRANSITOUS_GTFS_BASE_URL/$path"
}

const val TRANSITOUS_GTFS_BASE_URL = "https://api.transitous.org/gtfs"
