/** Loads human-readable country/region names from a bundled YAML asset. */

package dev.garado.transit.gtfs

import com.thelightphone.sdk.SealedLightContext
import org.yaml.snakeyaml.Yaml

private const val ASSET_PATH = "gtfs-to-human-readable.yaml"

class GtfsDisplayNames private constructor(
    private val countryNames: Map<String, String>,
    private val regionNames: Map<String, String>,
    private val agencyNames: Map<String, String>,
) {
    fun countryName(code: String): String = countryNames[code.uppercase()] ?: code.uppercase()

    fun regionName(code: String): String = regionNames[code.lowercase()] ?: code.uppercase()

    fun agencyName(key: String): String = agencyNames[key] ?: key

    companion object {
        @Volatile
        private var instance: GtfsDisplayNames? = null

        fun get(lightContext: SealedLightContext): GtfsDisplayNames =
            instance ?: synchronized(this) {
                instance ?: load(lightContext).also { instance = it }
            }

        @Suppress("UNCHECKED_CAST")
        private fun load(lightContext: SealedLightContext): GtfsDisplayNames {
            val yaml = try {
                val bytes = lightContext.readAsset(ASSET_PATH)
                Yaml().load<Map<String, Any>>(bytes.decodeToString())
            } catch (e: Exception) {
                null
            }
            val country = (yaml?.get("country") as? Map<String, String>).orEmpty()
            val region = (yaml?.get("region") as? Map<String, String>).orEmpty()
            val agency = (yaml?.get("agency") as? Map<String, String>).orEmpty()
            return GtfsDisplayNames(country, region, agency)
        }
    }
}
