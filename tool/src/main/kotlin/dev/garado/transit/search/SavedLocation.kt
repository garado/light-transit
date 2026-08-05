/** Domain model for saved locations */

package dev.garado.transit.search

data class SavedLocation(val id: Long = 0, val displayName: String, val result: LocationResult)
