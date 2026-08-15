package dev.garado.transit.data.api.nominatim

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import android.util.Log

/** Geocodes free-text search queries to coordinates via the public Nominatim (OpenStreetMap) API. */
object NominatimClient {
    private const val BASE_URL = "https://nominatim.openstreetmap.org/search"
    private const val USER_AGENT = "light-transit"
    private const val RESULT_LIMIT = "10"
    private const val CONNECT_TIMEOUT_MS = 5_000L
    private const val REQUEST_TIMEOUT_MS = 10_000L

    /** Half-width/height in degrees of the soft viewbox bias drawn around an origin */
    private const val VIEWBOX_DEGREE_DELTA = 0.5

    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MS
            requestTimeoutMillis = REQUEST_TIMEOUT_MS
        }
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /**
     * @param originLat/[originLon] optional location to softly bias results towards
     * (a "viewbox" the search prefers, without excluding matches outside of it)
     */
    suspend fun search(query: String, originLat: Double? = null, originLon: Double? = null): List<NominatimResult> {
        if (query.isBlank()) return emptyList()
        return try {
            client.get(BASE_URL) {
                header("User-Agent", USER_AGENT)
                parameter("q", query)
                parameter("format", "json")
                parameter("limit", RESULT_LIMIT)
                parameter("addressdetails", "1")
                if (originLat != null && originLon != null) {
                    val left = originLon - VIEWBOX_DEGREE_DELTA
                    val top = originLat + VIEWBOX_DEGREE_DELTA
                    val right = originLon + VIEWBOX_DEGREE_DELTA
                    val bottom = originLat - VIEWBOX_DEGREE_DELTA
                    parameter("viewbox", "$left,$top,$right,$bottom")
                    parameter("bounded", "0")
                }
            }.body()
        } catch (e: Exception) {
            Log.e("NominatimClient", "Search failed for query: $query", e)
            emptyList()
        }
    }
}
