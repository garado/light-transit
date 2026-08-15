package dev.garado.transit.data.database.searchcache

import dev.garado.transit.data.api.nominatim.NominatimResult
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

private val json = Json { ignoreUnknownKeys = true }

private const val TTL_MILLIS = 14L * 24 * 60 * 60 * 1000

/** Caches Nominatim search responses on disk, keyed by normalized query + origin, for [TTL_MILLIS]. */
internal class SearchCache(database: SearchCacheDatabase) {
    private val dao = database.searchCacheDao()

    suspend fun get(query: String, originLat: Double?, originLon: Double?): List<NominatimResult>? {
        val minCachedAt = System.currentTimeMillis() - TTL_MILLIS
        val entity = dao.get(cacheKey(query, originLat, originLon), minCachedAt) ?: return null
        return json.decodeFromString(entity.resultsJson)
    }

    suspend fun put(query: String, originLat: Double?, originLon: Double?, results: List<NominatimResult>) {
        dao.deleteExpired(minCachedAt = System.currentTimeMillis() - TTL_MILLIS)
        dao.upsert(
            SearchCacheEntity(
                key = cacheKey(query, originLat, originLon),
                resultsJson = json.encodeToString(results),
                cachedAt = System.currentTimeMillis(),
            )
        )
    }

    private fun cacheKey(query: String, originLat: Double?, originLon: Double?): String {
        val normalizedQuery = query.trim().lowercase()
        val origin = if (originLat != null && originLon != null) {
            "${roundForCacheKey(originLat)},${roundForCacheKey(originLon)}"
        } else {
            "none"
        }
        return "$normalizedQuery|$origin"
    }

    /** Rounds to ~1km precision so nearby origins (e.g. GPS jitter) share a cache entry */
    private fun roundForCacheKey(value: Double): Double = (value * 100).roundToInt() / 100.0
}
