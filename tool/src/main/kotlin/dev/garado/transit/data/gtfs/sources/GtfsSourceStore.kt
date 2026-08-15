/** Expose GtfsSource methods + data to rest of app */

package dev.garado.transit.data.gtfs.sources

import android.util.Log
import dev.garado.transit.data.api.transitous.GtfsDownloader
import dev.garado.transit.models.GtfsDataset
import dev.garado.transit.data.gtfs.local.GtfsCalendarTxtParser
import dev.garado.transit.data.gtfs.local.GtfsDatabase
import dev.garado.transit.data.gtfs.local.GtfsRoutesTxtParser
import dev.garado.transit.data.gtfs.local.GtfsShapesTxtParser
import dev.garado.transit.data.gtfs.local.GtfsSourceEntity
import dev.garado.transit.data.gtfs.local.GtfsStopTimesTxtParser
import dev.garado.transit.data.gtfs.local.GtfsStopsTxtParser
import dev.garado.transit.data.gtfs.local.GtfsTripsTxtParser
import dev.garado.transit.data.gtfs.local.GtfsZipExtractor
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "GtfsSourceStore"

/** Persists user-added GTFS sources on disk, downloads their .gtfs.zip, and imports data for local use */
internal class GtfsSourceStore(
    database: GtfsDatabase,
    private val filesDir: File,
    private val downloader: GtfsDownloader = GtfsDownloader.shared,
) {
    private val dao = database.gtfsSourceDao()
    private val stopsDao = database.gtfsStopsDao()
    private val scheduleDao = database.gtfsScheduleDao()

    val all: Flow<List<GtfsSource>> = dao.getAll().map { entities -> entities.map(GtfsSourceEntity::toGtfsSource) }

    suspend fun add(dataset: GtfsDataset) {
        val existing = dao.findByKeyAndRegion(dataset.key, dataset.regionCode)
        if (existing != null) {
            val state = runCatching { GtfsSourceDownloadState.valueOf(existing.downloadState) }
                .getOrDefault(GtfsSourceDownloadState.NOT_DOWNLOADED)
            if (state == GtfsSourceDownloadState.DOWNLOADED || state == GtfsSourceDownloadState.DOWNLOADING) {
                Log.d(TAG, "source ${dataset.key}/${dataset.regionCode} already $state (id=${existing.id}), skipping duplicate add")
                return
            }
            // NOT_DOWNLOADED or FAILED - retry the existing row instead of inserting a duplicate
            downloadFile(existing.id, dataset.downloadUrl)
            return
        }
        val id = dao.insert(GtfsSourceEntity(key = dataset.key, regionCode = dataset.regionCode, path = dataset.path))
        downloadFile(id, dataset.downloadUrl)
    }

    suspend fun retryDownload(source: GtfsSource) {
        downloadFile(source.id, source.downloadUrl)
    }

    /** Dependent stops/routes/trips/stop_times/calendar rows cascade automatically via foreign keys */
    suspend fun delete(source: GtfsSource) {
        GtfsImportProgressTracker.cancel(source.id)
        dao.deleteById(source.id)
        localFile(source.id).delete()
    }

    suspend fun deleteAll(sources: List<GtfsSource>) {
        sources.forEach { GtfsImportProgressTracker.cancel(it.id) }
        dao.deleteByIds(sources.map { it.id })
        sources.forEach { localFile(it.id).delete() }
    }

    /** Always resolves to DOWNLOADED or FAILED, unless cancelled */
    private suspend fun downloadFile(id: Long, url: String): Unit = coroutineScope {
        val job = launch {
            dao.updateDownloadState(id, GtfsSourceDownloadState.DOWNLOADING.name)
            val destination = localFile(id)
            Log.d(TAG, "download started for source $id ($url)")
            GtfsImportProgressTracker.update(id, GtfsImportStage.Downloading(percent = null))
            val success = try {
                downloader.download(url, destination) { percent ->
                    GtfsImportProgressTracker.update(id, GtfsImportStage.Downloading(percent))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed for source $id", e)
                false
            }
            Log.d(TAG, "download stopped for source $id, success=$success")
            if (success) {
                try {
                    importStops(id, destination)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "stops.txt import failed for source $id", e)
                }
                try {
                    importSchedule(id, destination)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "schedule import failed for source $id", e)
                }
            }
            dao.updateDownloadState(id, (if (success) GtfsSourceDownloadState.DOWNLOADED else GtfsSourceDownloadState.FAILED).name)
            GtfsImportProgressTracker.clear(id)
        }
        GtfsImportProgressTracker.registerJob(id, job)
        job.invokeOnCompletion { GtfsImportProgressTracker.unregisterJob(id) }
        job.join()
    }

    /** Best-effort (parse failures don't undo a successful download) */
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

    /** Best-effort (parse failures don't undo a successful download) */
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

        Log.d(TAG, "shapes.txt parsing started for source $id")
        GtfsImportProgressTracker.update(id, GtfsImportStage.ParsingShapes(count = 0))
        var shapePointsImported = 0
        val shapePointCount = scheduleDao.insertShapePointsInOneTransaction {
            withContext(Dispatchers.IO) {
                GtfsZipExtractor.readEntry(zipFile, "shapes.txt") { reader ->
                    GtfsShapesTxtParser.parse(id, reader) { batch ->
                        scheduleDao.insertShapePoints(batch)
                        shapePointsImported += batch.size
                        GtfsImportProgressTracker.update(id, GtfsImportStage.ParsingShapes(shapePointsImported))
                    }
                }
            } ?: 0
        }
        Log.d(TAG, "shapes.txt parsing ended: $shapePointCount shape points for source $id")

        Log.d(
            TAG,
            "schedule parsing ended: ${routes.size} routes, ${trips.size} trips, $stopTimeCount stop_times, " +
                "${calendars.size} calendar rows, $shapePointCount shape points for source $id",
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
