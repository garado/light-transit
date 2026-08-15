package dev.garado.transit.data.gtfs.sources

import dev.garado.transit.models.gtfsDownloadUrl

enum class GtfsSourceDownloadState {
    NOT_DOWNLOADED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED;

    /** What to show next to a source in this state, or null to show nothing */
    val statusLabel: String?
        get() = when (this) {
            NOT_DOWNLOADED -> "Not downloaded - tap to download"
            DOWNLOADED -> null
            DOWNLOADING -> "Downloading..."
            FAILED -> "Download failed - tap to retry"
        }

    val isRetryable: Boolean get() = this == FAILED || this == NOT_DOWNLOADED
}

data class GtfsSource(
    val id: Long,
    val key: String,
    val regionCode: String,
    val path: String,
    val downloadState: GtfsSourceDownloadState,
) {
    val downloadUrl: String get() = gtfsDownloadUrl(path)
}
