package dev.garado.transit.data.api.nominatim

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Structured breakdown of a result's address, returned when the request sets `addressdetails=1` */
@Serializable
data class NominatimAddress(
    @SerialName("house_number") val houseNumber: String? = null,
    val road: String? = null,
    val city: String? = null,
    val town: String? = null,
    val village: String? = null,
    val state: String? = null,
    val postcode: String? = null,
    val country: String? = null,
)

/** Joins the most relevant address parts into a single display line, e.g. "123 Main St, Oakland, CA 94612" */
fun NominatimAddress.toDisplayLine(): String {
    val streetLine = listOfNotNull(houseNumber, road).joinToString(" ").ifBlank { null }
    val locality = city ?: town ?: village
    val stateAndPostcode = listOfNotNull(state, postcode).joinToString(" ").ifBlank { null }

    return listOfNotNull(streetLine, locality, stateAndPostcode, country).joinToString(", ")
}
