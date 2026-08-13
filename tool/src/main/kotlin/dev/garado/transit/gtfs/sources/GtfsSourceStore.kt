/** Expose GtfsSource methods + data to rest of app */

package dev.garado.transit.gtfs.sources

import android.util.Log
import dev.garado.transit.gtfs.GtfsDataset
import dev.garado.transit.gtfs.local.GtfsCalendarTxtParser
import dev.garado.transit.gtfs.local.GtfsRoutesTxtParser
import dev.garado.transit.gtfs.local.GtfsScheduleDao
import dev.garado.transit.gtfs.local.GtfsStopTimesTxtParser
import dev.garado.transit.gtfs.local.GtfsStopsDao
import dev.garado.transit.gtfs.local.GtfsStopsTxtParser
import dev.garado.transit.gtfs.local.GtfsTripsTxtParser
import dev.garado.transit.gtfs.local.GtfsZipExtractor
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private const val TAG = "GtfsSourceStore"

/** Persists user-added GTFS sources on disk, downloads their .gtfs.zip, and imports schedule data for local use */
internal class GtfsSourceStore(
    database: GtfsSourceDatabase,
    private val filesDir: File,
    private val stopsDao: GtfsStopsDao,
    private val scheduleDao: GtfsScheduleDao,
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
        scheduleDao.deleteBySource(source.id)
        localFile(source.id).delete()
    }

    suspend fun deleteAll(sources: List<GtfsSource>) {
        dao.deleteByIds(sources.map { it.id })
        sources.forEach {
            stopsDao.deleteBySource(it.id)
            scheduleDao.deleteBySource(it.id)
            localFile(it.id).delete()
        }
    }

    /** Always resolves to DOWNLOADED or FAILED */
    private suspend fun downloadFile(id: Long, url: String) {
        dao.updateDownloadState(id, GtfsSourceDownloadState.DOWNLOADING.name)
        val destination = localFile(id)
        Log.d(TAG, "download started for source $id ($url)")
        GtfsImportProgressTracker.update(id, GtfsImportStage.Downloading(percent = null))
        val success = try {
            downloader.download(url, destination) { percent ->
                GtfsImportProgressTracker.update(id, GtfsImportStage.Downloading(percent))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for source $id", e)
            false
        }
        Log.d(TAG, "download stopped for source $id, success=$success")
        if (success) {
            try {
                importStops(id, destination)
            } catch (e: Exception) {
                Log.e(TAG, "stops.txt import failed for source $id", e)
            }
            try {
                importSchedule(id, destination)
            } catch (e: Exception) {
                Log.e(TAG, "schedule import failed for source $id", e)
            }
        }
        dao.updateDownloadState(id, (if (success) GtfsSourceDownloadState.DOWNLOADED else GtfsSourceDownloadState.FAILED).name)
        GtfsImportProgressTracker.clear(id)
    }

    /** Best-effort (a stops.txt parse failure shouldn't undo an otherwise-successful download) */
    private suspend fun importStops(id: Long, zipFile: File) {
        Log.d(TAG, "stops.txt parsing started for source $id")
        GtfsImportProgressTracker.update(id, GtfsImportStage.ParsingStops)
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
        Log.d(TAG, "stops.txt parsing ended: imported ${stops.size} stops for source $id")
    }

    /** Best-effort (a schedule parse failure shouldn't undo an otherwise-successful download) */
    private suspend fun importSchedule(id: Long, zipFile: File) {
        Log.d(TAG, "schedule parsing started for source $id")

        Log.d(TAG, "routes.txt parsing started for source $id")
        GtfsImportProgressTracker.update(id, GtfsImportStage.ParsingRoutes)
        val routes = withContext(Dispatchers.IO) {
            GtfsZipExtractor.readEntry(zipFile, "routes.txt") { reader -> GtfsRoutesTxtParser.parse(id, reader) }
        }.orEmpty()
        Log.d(TAG, "routes.txt parsing ended: ${routes.size} routes for source $id")

        Log.d(TAG, "trips.txt parsing started for source $id")
        GtfsImportProgressTracker.update(id, GtfsImportStage.ParsingTrips)
        val trips = withContext(Dispatchers.IO) {
            GtfsZipExtractor.readEntry(zipFile, "trips.txt") { reader -> GtfsTripsTxtParser.parse(id, reader) }
        }.orEmpty()
        Log.d(TAG, "trips.txt parsing ended: ${trips.size} trips for source $id")

        Log.d(TAG, "calendar.txt parsing started for source $id")
        GtfsImportProgressTracker.update(id, GtfsImportStage.ParsingCalendar)
        val calendars = withContext(Dispatchers.IO) {
            GtfsZipExtractor.readEntry(zipFile, "calendar.txt") { reader -> GtfsCalendarTxtParser.parse(id, reader) }
        }.orEmpty()
        Log.d(TAG, "calendar.txt parsing ended: ${calendars.size} calendar rows for source $id")

        scheduleDao.deleteBySource(id)
        scheduleDao.insertRoutes(routes)
        scheduleDao.insertTrips(trips)
        scheduleDao.insertCalendars(calendars)

        Log.d(TAG, "stop_times.txt parsing started for source $id")
        GtfsImportProgressTracker.update(id, GtfsImportStage.ParsingStopTimes(count = 0))
        var stopTimesImported = 0
        val stopTimeCount = scheduleDao.insertStopTimesInOneTransaction {
            withContext(Dispatchers.IO) {
                GtfsZipExtractor.readEntry(zipFile, "stop_times.txt") { reader ->
                    GtfsStopTimesTxtParser.parse(id, reader) { batch ->
                        scheduleDao.insertStopTimes(batch)
                        stopTimesImported += batch.size
                        GtfsImportProgressTracker.update(id, GtfsImportStage.ParsingStopTimes(stopTimesImported))
                    }
                }
            } ?: 0
        }
        Log.d(TAG, "stop_times.txt parsing ended: $stopTimeCount stop_times for source $id")

        Log.d(
            TAG,
            "schedule parsing ended: ${routes.size} routes, ${trips.size} trips, $stopTimeCount stop_times, " +
                "${calendars.size} calendar rows for source $id",
        )
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
