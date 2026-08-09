package dev.garado.transit.gtfs

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import org.yaml.snakeyaml.Yaml

private const val CONFIG_URL = "$TRANSITOUS_GTFS_BASE_URL/config.yml"
private const val TAG = "TransitousFeedFetcher"

/** Fetches and parses Transitous's dataset catalog (config.yml). */
class TransitousFeedFetcher {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000L
            requestTimeoutMillis = 30_000L
        }
    }

    /** Raw config.yml text, or null if the request failed. */
    suspend fun fetchRawConfig(): String? = try {
        client.get(CONFIG_URL).body()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch $CONFIG_URL", e)
        null
    }

    fun parseDatasets(yamlText: String): List<GtfsDataset> {
        val root = try {
            Yaml().load<Map<String, Any?>>(yamlText)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse config.yml", e)
            return emptyList()
        }
        val timetable = root?.get("timetable") as? Map<*, *> ?: return emptyList()
        val datasets = timetable["datasets"] as? Map<*, *> ?: return emptyList()

        return datasets.mapNotNull { (key, value) ->
            val entry = value as? Map<*, *> ?: return@mapNotNull null
            val path = entry["path"] as? String ?: return@mapNotNull null
            val regionCode = path.substringBefore("_", missingDelimiterValue = "")
            if (key !is String || regionCode.isEmpty()) return@mapNotNull null
            GtfsDataset(key = key, regionCode = regionCode, path = path)
        }
    }
}
