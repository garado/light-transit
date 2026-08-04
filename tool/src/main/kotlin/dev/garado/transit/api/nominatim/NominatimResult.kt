package dev.garado.transit.api.nominatim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NominatimResult(
    @SerialName("display_name") val displayName: String,
    val lat: String,
    val lon: String,
    val address: NominatimAddress? = null,
)
