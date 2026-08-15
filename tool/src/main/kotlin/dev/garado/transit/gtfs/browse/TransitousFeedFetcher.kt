package dev.garado.transit.gtfs.browse

import android.util.Log
import dev.garado.transit.models.GtfsDataset
import dev.garado.transit.models.TRANSITOUS_GTFS_BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import org.yaml.snakeyaml.Yaml

private const val CONFIG_URL = "$TRANSITOUS_GTFS_BASE_URL/config.yml"
private const val INDEX_URL = "$TRANSITOUS_GTFS_BASE_URL/"
private const val TAG = "TransitousFeedFetcher"

/** A standard nginx/Apache autoindex row: `<a href="FILENAME">...</a>  DATE TIME  SIZE` */
private val INDEX_ROW_REGEX = Regex("""<a href="([^"]+\.gtfs\.zip)">.*?(\d+)\s*$""", RegexOption.MULTILINE)

/** Fetches and parses Transitous's dataset catalog (config.yml + the directory index for sizes). */
class TransitousFeedFetcher {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000L
            requestTimeoutMillis = 30_000L
        }
    }

    /** Raw config.yml text, or null if the request failed. */
    suspend fun fetchRawConfig(): String? = fetchText(CONFIG_URL)

    /** Raw directory index HTML (has file sizes config.yml doesn't), or null if the request failed. */
    suspend fun fetchRawIndex(): String? = fetchText(INDEX_URL)

    private suspend fun fetchText(url: String): String? = try {
        client.get(url).body()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch $url", e)
        null
    }

    fun parseDatasets(yamlText: String, indexHtml: String? = null): List<GtfsDataset> {
        val root = try {
            Yaml().load<Map<String, Any?>>(yamlText)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse config.yml", e)
            return emptyList()
        }
        val timetable = root?.get("timetable") as? Map<*, *> ?: return emptyList()
        val datasets = timetable["datasets"] as? Map<*, *> ?: return emptyList()
        val sizesByPath = indexHtml?.let { parseIndexSizes(it) } ?: emptyMap()

        return datasets.mapNotNull { (key, value) ->
            val entry = value as? Map<*, *> ?: return@mapNotNull null
            val path = entry["path"] as? String ?: return@mapNotNull null
            val regionCode = path.substringBefore("_", missingDelimiterValue = "")
            if (key !is String || regionCode.isEmpty()) return@mapNotNull null
            GtfsDataset(key = key, regionCode = regionCode, path = path, sizeBytes = sizesByPath[path])
        }
    }

    private fun parseIndexSizes(indexHtml: String): Map<String, Long> =
        INDEX_ROW_REGEX.findAll(indexHtml).associate { match ->
            val (encodedFilename, size) = match.destructured
            java.net.URLDecoder.decode(encodedFilename, "UTF-8") to size.toLong()
        }
}
