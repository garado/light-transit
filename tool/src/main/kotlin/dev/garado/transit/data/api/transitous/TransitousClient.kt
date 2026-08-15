/**
 * Shared client for Transitous (MOTIS) routing API: https://transitous.org
 */

package dev.garado.transit.data.api.transitous

import android.util.Log
import com.thelightphone.sdk.SealedLightContext
import dev.garado.transit.BuildConfig
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
enum class TransitousEndpoint(val path: String, val mockAsset: String) {
    PLAN("/api/v6/plan", "mocks/transitous/v6-plan.json"),
}

class TransitousClient(internal val lightContext: SealedLightContext) {
    internal val usageTracker = TransitousUsageTracker(lightContext.dataStore)
    internal val mockSettings = MockTransitousSettings(lightContext.dataStore)

    internal suspend inline fun <reified T> request(endpoint: TransitousEndpoint, params: Map<String, Any?>): T? {
        if (mockSettings.simulateApiFailure.first()) {
            Log.w(TAG, "Simulating API failure for ${endpoint.path} (dev setting)")
            return null
        }
        return if (mockSettings.useMockedResponses.first()) {
            mockResponse(endpoint)
        } else {
            realResponse(endpoint, params)
        }
    }

    internal inline fun <reified T> mockResponse(endpoint: TransitousEndpoint): T? = try {
        val bytes = lightContext.readAsset(endpoint.mockAsset)
        json.decodeFromString(bytes.decodeToString())
    } catch (e: Exception) {
        Log.e(TAG, "Failed to read mock response for ${endpoint.path}", e)
        null
    }

    internal suspend inline fun <reified T> realResponse(endpoint: TransitousEndpoint, params: Map<String, Any?>): T? {
        if (usageTracker.callCountThisMonth.first() >= TransitousUsageTracker.MONTHLY_CALL_LIMIT) {
            Log.w(TAG, "Monthly call limit reached (${TransitousUsageTracker.MONTHLY_CALL_LIMIT}), skipping request for ${endpoint.path}")
            return null
        }
        if (!TransitousRateLimiter.tryAcquire()) {
            Log.w(TAG, "Rate limit exceeded, skipping request for ${endpoint.path}")
            return null
        }
        return try {
            val response: T = client.get(BASE_URL + endpoint.path) {
                header("accept", "application/json")
                header("User-Agent", USER_AGENT)
                params.forEach { (key, value) -> parameter(key, value) }
            }.body()
            usageTracker.recordApiCall()
            response
        } catch (e: Exception) {
            Log.e(TAG, "Transitous request failed for ${endpoint.path}", e)
            null
        }
    }

    internal companion object {
        const val TAG = "TransitousClient"
        const val BASE_URL = "https://api.transitous.org"
        val USER_AGENT = "light-transit/${BuildConfig.VERSION_NAME} (https://github.com/garado/light-transit)"
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
