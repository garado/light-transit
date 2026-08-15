/** (Developer settings) wipe all local GTFS state */

package dev.garado.transit.gtfs.sources

import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.gtfs.browse.GtfsCatalogDatabaseHolder
import dev.garado.transit.gtfs.local.GtfsDatabaseHolder
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Clears the Transitous browse cache and deletes every downloaded .gtfs.zip.
 * Keeps saved sources, but resets them to NOT_DOWNLOADED
 */
suspend fun clearGtfsDownloadCache(lightContext: SealedLightContext) {
    GtfsCatalogDatabaseHolder.get(lightContext).gtfsCatalogDao().clear()
    val gtfsDb = GtfsDatabaseHolder.get(lightContext)
    gtfsDb.gtfsStopsDao().clear()
    gtfsDb.gtfsScheduleDao().clear()

    withContext(Dispatchers.IO) {
        File(lightContext.filesDir, "gtfs").listFiles()?.forEach { it.delete() }
    }

    gtfsDb.gtfsSourceDao().resetAllDownloadStates(GtfsSourceDownloadState.NOT_DOWNLOADED.name)
}
