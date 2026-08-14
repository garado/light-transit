package dev.garado.transit.location

import android.util.Log
import dev.garado.transit.api.UserLocationInterface
import dev.garado.transit.api.models.UserLocation
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

private const val TAG = "UserLocationLogsProvider"
private const val EVENT_MARKER = "LightOSGeolocationModule:didReceiveLocationUpdate"

/**
 * Insanely jank way to obtain user's current location
 *
 * light-sdk doesn't support fetching user's current location yet.
 * As a stopgap this reads ther user's location from LightOSEventManager's geolocation logs lines.
 *
 * Requires granting permissions:
 *   adb shell pm grant dev.garado.transit android.permission.READ_LOGS
 * 
 * Also requires LightOS to be actively polling location (can briefly open
 * Directions tool for that)
 */
class UserLocationLogsProvider : UserLocationInterface {
    private val _location = MutableStateFlow<UserLocation?>(null)
    override val location: Flow<UserLocation?> = _location.asStateFlow()

    /** Tails logcat until cancelled; suspends for the lifetime of the read loop */
    suspend fun start() = withContext(Dispatchers.IO) {
        val process = try {
            ProcessBuilder("logcat", "-v", "brief", "-s", "LightOSEventManager:D").start()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start logcat process", e)
            return@withContext
        }
        try {
            BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                lines.forEach { line -> parseLocationEvent(line)?.let { _location.value = it } }
            }
        } finally {
            process.destroy()
        }
    }
}

/**
 * Sample log line:
 *
 * LightOSEventManager: Sending event LightOSGeolocationModule:didReceiveLocationUpdate{
 *    "altitude":31.3,
 *    "realAccuracy":10.7,
 *    "provider":gps,
 *    "latitude":-38.8,
 *    "accuracy":0.0,
 *    "technology":gnss,
 *    "speed":0.0,
 *    "timestamp":1786674833763,
 *    "longitude":130.4
 * } */
internal fun parseLocationEvent(line: String): UserLocation? {
    val markerIndex = line.indexOf(EVENT_MARKER)
    if (markerIndex < 0) return null
    val payloadStart = line.indexOf('{', markerIndex)
    if (payloadStart < 0) return null
    val payload = line.substring(payloadStart)

    val lat = extractDouble(payload, "latitude") ?: return null
    val lon = extractDouble(payload, "longitude") ?: return null
    val timestamp = extractLong(payload, "timestamp") ?: return null

    return UserLocation(
        lat = lat,
        lon = lon,
        accuracyMeters = extractDouble(payload, "accuracy") ?: 0.0,
        altitudeMeters = extractDouble(payload, "altitude") ?: 0.0,
        speedMetersPerSecond = extractDouble(payload, "speed") ?: 0.0,
        timestamp = timestamp,
        technology = extractRaw(payload, "technology") ?: "unknown",
    )
}

private fun extractDouble(payload: String, key: String): Double? = extractRaw(payload, key)?.toDoubleOrNull()

private fun extractLong(payload: String, key: String): Long? = extractRaw(payload, key)?.toLongOrNull()

/** payload is not valid JSON (unquoted string values), so pull the raw text after "key": up to the next , or } */
private fun extractRaw(payload: String, key: String): String? {
    val marker = "\"$key\":"
    val start = payload.indexOf(marker)
    if (start < 0) return null
    val valueStart = start + marker.length
    var end = valueStart
    while (end < payload.length && payload[end] != ',' && payload[end] != '}') end++
    return payload.substring(valueStart, end).trim().trim('"').ifEmpty { null }
}
