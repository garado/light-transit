package dev.garado.transit.search

sealed interface DepartureSelection {
    data object Now : DepartureSelection
    data class LeaveAt(val hour24: Int, val minute: Int) : DepartureSelection
    data class ArriveBy(val hour24: Int, val minute: Int) : DepartureSelection
}
