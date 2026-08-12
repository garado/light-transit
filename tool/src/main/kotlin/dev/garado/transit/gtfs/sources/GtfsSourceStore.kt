/** Expose GtfsSource methods + data to rest of app */

package dev.garado.transit.gtfs.sources

import android.util.Log
import dev.garado.transit.gtfs.GtfsDataset
import dev.garado.transit.gtfs.local.GtfsStopsDao
import dev.garado.transit.gtfs.local.GtfsStopsTxtParser
import dev.garado.transit.gtfs.local.GtfsZipExtractor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "GtfsSourceStore"

/** Persists user-added GTFS sources on disk, downloads their .gtfs.zip, and imports stops.txt for local search */
internal class GtfsSourceStore(
    database: GtfsSourceDatabase,
    private val filesDir: File,
    private val stopsDao: GtfsStopsDao,
    private val downloader: GtfsDownloader = GtfsDownloader.shared,
) {
    private val dao = database.gtfsSourceDao()

    val all: Flow<List<GtfsSource>> = dao.getAll().map { entities -> entities.map(GtfsSourceEntity::toGtfsSource) }

    suspend fun add(dataset: GtfsDataset) {
        val id = dao.insert(GtfsSourceEntity(key = dataset.key, regionCode = dataset.regionCode, path = dataset.path))
        downloadFile(id, dataset.downloadUrl)
    }

    suspend fun retryDownload(source: GtfsSource) {
        downloadFile(source.id, source.downloadUrl)
    }

    suspend fun delete(source: GtfsSource) {
        dao.deleteById(source.id)
        stopsDao.deleteBySource(source.id)
        localFile(source.id).delete()
    }

    suspend fun deleteAll(sources: List<GtfsSource>) {
        dao.deleteByIds(sources.map { it.id })
        sources.forEach {
            stopsDao.deleteBySource(it.id)
            localFile(it.id).delete()
        }
    }

    /** Always resolves to DOWNLOADED or FAILED */
    private suspend fun downloadFile(id: Long, url: String) {
        dao.updateDownloadState(id, GtfsSourceDownloadState.DOWNLOADING.name)
        val destination = localFile(id)
        val success = try {
            downloader.download(url, destination)
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for source $id", e)
            false
        }
        if (success) {
            try {
                importStops(id, destination)
            } catch (e: Exception) {
                Log.e(TAG, "stops.txt import failed for source $id", e)
            }
        }
        dao.updateDownloadState(id, (if (success) GtfsSourceDownloadState.DOWNLOADED else GtfsSourceDownloadState.FAILED).name)
    }

    /** Best-effort (a stops.txt parse failure shouldn't undo an otherwise-successful download) */
    private suspend fun importStops(id: Long, zipFile: File) {
        val stops = withContext(Dispatchers.IO) {
            GtfsZipExtractor.readEntry(zipFile, "stops.txt") { reader -> GtfsStopsTxtParser.parse(id, reader) }
        }
        if (stops == null) {
            Log.w(TAG, "stops.txt not found in zip for source $id")
            return
        }
        if (stops.isEmpty()) {
            Log.w(TAG, "stops.txt parsed to zero rows for source $id")
            return
        }
        stopsDao.deleteBySource(id)
        stopsDao.insertAll(stops)
        Log.d(TAG, "imported ${stops.size} stops for source $id")
    }

    private fun localFile(id: Long) = File(File(filesDir, "gtfs"), "$id.gtfs.zip")
}

private fun GtfsSourceEntity.toGtfsSource() = GtfsSource(
    id = id,
    key = key,
    regionCode = regionCode,
    path = path,
    downloadState = runCatching { GtfsSourceDownloadState.valueOf(downloadState) }.getOrDefault(GtfsSourceDownloadState.NOT_DOWNLOADED),
)
