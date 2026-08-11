/** (Developer settings) wipe all local GTFS state */

package dev.garado.transit.gtfs

import com.thelightphone.sdk.SealedLightContext
import java.io.File

/**
 * Clears the Transitous browse cache and deletes every downloaded .gtfs.zip.
 * Keeps saved sources, but resets them to NOT_DOWNLOADED
 */
suspend fun clearGtfsDownloadCache(lightContext: SealedLightContext) {
    GtfsCatalogDatabaseHolder.get(lightContext).gtfsCatalogDao().clear()

    File(lightContext.filesDir, "gtfs").listFiles()?.forEach { it.delete() }

    GtfsSourceDatabaseHolder.get(lightContext).gtfsSourceDao()
        .resetAllDownloadStates(GtfsSourceDownloadState.NOT_DOWNLOADED.name)
}
