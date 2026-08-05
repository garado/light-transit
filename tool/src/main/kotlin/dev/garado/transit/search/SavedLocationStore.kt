/** Expose SavedLocation methods + data to rest of app */

package dev.garado.transit.search

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists user-saved locations on disk */
internal class SavedLocationStore(database: SavedLocationDatabase) {
    private val dao = database.savedLocationDao()

    val all: Flow<List<SavedLocation>> = dao.getAll().map { entities -> entities.map(SavedLocationEntity::toSavedLocation) }

    suspend fun add(displayName: String, result: LocationResult) {
        dao.insert(
            SavedLocationEntity(
                displayName = displayName,
                title = result.title,
                address = result.address,
                lat = result.lat,
                lon = result.lon,
            )
        )
    }

    suspend fun delete(savedLocation: SavedLocation) {
        dao.delete(
            SavedLocationEntity(
                id = savedLocation.id,
                displayName = savedLocation.displayName,
                title = savedLocation.result.title,
                address = savedLocation.result.address,
                lat = savedLocation.result.lat,
                lon = savedLocation.result.lon,
            )
        )
    }
}

private fun SavedLocationEntity.toSavedLocation() = SavedLocation(
    id = id,
    displayName = displayName,
    result = LocationResult(title = title, address = address, lat = lat, lon = lon),
)
