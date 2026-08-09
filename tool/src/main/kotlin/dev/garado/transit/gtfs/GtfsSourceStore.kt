/** Expose GtfsSource methods + data to rest of app */

package dev.garado.transit.gtfs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Persists user-added GTFS sources on disk */
internal class GtfsSourceStore(database: GtfsSourceDatabase) {
    private val dao = database.gtfsSourceDao()

    val all: Flow<List<GtfsSource>> = dao.getAll().map { entities -> entities.map(GtfsSourceEntity::toGtfsSource) }

    suspend fun add(dataset: GtfsDataset) {
        dao.insert(GtfsSourceEntity(key = dataset.key, regionCode = dataset.regionCode, path = dataset.path))
    }

    suspend fun delete(source: GtfsSource) {
        dao.deleteById(source.id)
    }
}

private fun GtfsSourceEntity.toGtfsSource() = GtfsSource(id = id, key = key, regionCode = regionCode, path = path)
