/** Expose GtfsSource methods + data to rest of app */

package dev.garado.transit.gtfs

import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists user-added GTFS sources on disk, and downloads their .gtfs.zip alongside them */
internal class GtfsSourceStore(
    database: GtfsSourceDatabase,
    private val filesDir: File,
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
        localFile(source.id).delete()
    }

    suspend fun deleteAll(sources: List<GtfsSource>) {
        dao.deleteByIds(sources.map { it.id })
        sources.forEach { localFile(it.id).delete() }
    }

    private suspend fun downloadFile(id: Long, url: String) {
        dao.updateDownloadState(id, GtfsSourceDownloadState.DOWNLOADING.name)
        val success = downloader.download(url, localFile(id))
        dao.updateDownloadState(id, (if (success) GtfsSourceDownloadState.DOWNLOADED else GtfsSourceDownloadState.FAILED).name)
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
