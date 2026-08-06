package dev.garado.transit.search

import dev.garado.transit.nextEpochSecondsFor

sealed interface DepartureSelection {
    data object Now : DepartureSelection
    data class LeaveAt(val hour24: Int, val minute: Int) : DepartureSelection
    data class ArriveBy(val hour24: Int, val minute: Int) : DepartureSelection
}

/** (leaveTime, arrivalTime) epoch seconds to pass to [dev.garado.transit.api.transit.TransitClient.plan]. */
fun DepartureSelection.toApiTimeParams(): Pair<Long?, Long?> = when (this) {
    DepartureSelection.Now -> null to null
    is DepartureSelection.LeaveAt -> nextEpochSecondsFor(hour24, minute) to null
    is DepartureSelection.ArriveBy -> null to nextEpochSecondsFor(hour24, minute)
}
