package dev.garado.transit.gtfs.sources

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stage of an in-progress GTFS source download+import (for UI feedback) */
sealed class GtfsImportStage {
    data class Downloading(val percent: Int?) : GtfsImportStage()
    data object ParsingStops : GtfsImportStage()
    data object ParsingRoutes : GtfsImportStage()
    data object ParsingTrips : GtfsImportStage()
    data object ParsingCalendar : GtfsImportStage()
    data class ParsingStopTimes(val count: Int) : GtfsImportStage()
}

/**
 * Process-wide in-memory import progress per source, keyed by source id
 */
object GtfsImportProgressTracker {
    private val _progress = MutableStateFlow<Map<Long, GtfsImportStage>>(emptyMap())
    val progress: StateFlow<Map<Long, GtfsImportStage>> = _progress.asStateFlow()

    fun update(sourceId: Long, stage: GtfsImportStage) {
        _progress.value = _progress.value + (sourceId to stage)
    }

    fun clear(sourceId: Long) {
        _progress.value = _progress.value - sourceId
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
    }
