package dev.garado.transit.data.gtfs.sources

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Stage of an in-progress GTFS source download+import (for UI feedback) */
sealed class GtfsImportStage {
    data class Downloading(val percent: Int?) : GtfsImportStage()
    data object ParsingStops : GtfsImportStage()
    data object ParsingRoutes : GtfsImportStage()
    data object ParsingTrips : GtfsImportStage()
    data object ParsingCalendar : GtfsImportStage()
    data class ParsingStopTimes(val count: Int) : GtfsImportStage()
    data class ParsingShapes(val count: Int) : GtfsImportStage()
}

/**
 * Process-wide in-memory import progress per source, keyed by source id
 */
object GtfsImportProgressTracker {
    private val _progress = MutableStateFlow<Map<Long, GtfsImportStage>>(emptyMap())
    val progress: StateFlow<Map<Long, GtfsImportStage>> = _progress.asStateFlow()

    private val jobs = mutableMapOf<Long, Job>()

    fun update(sourceId: Long, stage: GtfsImportStage) {
        _progress.update { it + (sourceId to stage) }
    }

    fun clear(sourceId: Long) {
        _progress.update { it - sourceId }
    }

    /** Save [Job] doing [sourceId]'s download+import in case it needs to be cancelled */
    @Synchronized
    fun registerJob(sourceId: Long, job: Job) {
        jobs[sourceId] = job
    }

    @Synchronized
    fun unregisterJob(sourceId: Long) {
        jobs.remove(sourceId)
    }

    @Synchronized
    fun cancel(sourceId: Long) {
        jobs[sourceId]?.cancel()
    }
}

val GtfsImportStage.label: String
    get() = when (this) {
        is GtfsImportStage.Downloading -> if (percent != null) "Downloading... $percent%" else "Downloading..."
        GtfsImportStage.ParsingStops -> "Parsing stops"
        GtfsImportStage.ParsingRoutes -> "Parsing routes"
        GtfsImportStage.ParsingTrips -> "Parsing trips"
        GtfsImportStage.ParsingCalendar -> "Parsing calendar"
        is GtfsImportStage.ParsingStopTimes -> "${count / 1000}k stop times imported"
        is GtfsImportStage.ParsingShapes -> "${count / 1000}k shape points imported"
    }
