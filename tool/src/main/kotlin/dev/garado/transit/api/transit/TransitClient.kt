/**
 * Client for TransitAPI: https://api-doc.transitapp.com/v4.html
 * 1500 calls/month; 5 calls/min
 */

package dev.garado.transit.api.transit

import android.util.Log
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.BuildConfig
import dev.garado.transit.api.transit.models.PlanApiResponse
import dev.garado.transit.api.transit.models.TripPlan
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/** Map endpoints to their mocked responses */
private enum class TransitEndpoint(val path: String, val mockAsset: String) {
    PLAN("/v4/public/plan", "mock_transit_plan.json"),
}

class TransitClient(private val lightContext: SealedLightContext) {
    private val usageTracker = TransitApiUsageTracker(lightContext.dataStore)
    private val mockSettings = MockTransitApiSettings(lightContext.dataStore)

    /** https://api-doc.transitapp.com/v4.html#GET/v4/public/plan */
    suspend fun plan(
        fromLat: Double,
        fromLon: Double,
        toLat: Double,
        toLon: Double,
        leaveTime: Long = System.currentTimeMillis() / 1000,
    ): List<TripPlan> {
        val response: PlanApiResponse? = request(
            endpoint = TransitEndpoint.PLAN,
            params = mapOf(
                "from_lat" to fromLat,
                "from_lon" to fromLon,
                "to_lat" to toLat,
                "to_lon" to toLon,
                "mode" to "transit",
                "leave_time" to leaveTime,
            ),
        )
        return response?.toTripPlans() ?: emptyList()
    }

    private suspend inline fun <reified T> request(endpoint: TransitEndpoint, params: Map<String, Any?>): T? =
        if (mockSettings.useMockedResponses.first()) {
            mockResponse(endpoint)
        } else {
            realResponse(endpoint, params)
        }

    private inline fun <reified T> mockResponse(endpoint: TransitEndpoint): T? = try {
        val bytes = lightContext.readAsset(endpoint.mockAsset)
        json.decodeFromString(bytes.decodeToString())
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read mock response for ${endpoint.path}", e)
        null
    }

    private suspend inline fun <reified T> realResponse(endpoint: TransitEndpoint, params: Map<String, Any?>): T? {
        if (usageTracker.callCountThisMonth.first() >= TransitApiUsageTracker.MONTHLY_CALL_LIMIT) {
            Log.w(TAG, "Monthly call limit reached (${TransitApiUsageTracker.MONTHLY_CALL_LIMIT}), skipping request for ${endpoint.path}")
            return null
        }
        if (!TransitRateLimiter.tryAcquire()) {
            Log.w(TAG, "Rate limit exceeded (5 calls/min), skipping request for ${endpoint.path}")
            return null
        }
        return try {
            val response: T = client.get(BASE_URL + endpoint.path) {
                header("apiKey", BuildConfig.TRANSIT_API_KEY)
                header("accept", "application/json")
                params.forEach { (key, value) -> parameter(key, value) }
            }.body()
            usageTracker.recordApiCall()
            response
        } catch (e: Exception) {
            Log.e(TAG, "Transit API request failed for ${endpoint.path}", e)
            null
        }
    }

    private companion object {
        const val TAG = "TransitClient"
        const val BASE_URL = "https://external.transitapp.com"
        const val CONNECT_TIMEOUT_MS = 5_000L
        const val REQUEST_TIMEOUT_MS = 10_000L

        val json = Json { ignoreUnknownKeys = true }

        val client = HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
            install(ContentNegotiation) {
                json(json)
            }
        }
    }
}
