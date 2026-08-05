package dev.garado.transit.route

import kotlin.test.Test
import kotlin.test.assertEquals

class PolylineDecoderTest {

    @Test
    fun `decodes Google's own reference example`() {
        // https://developers.google.com/maps/documentation/utilities/polylinealgorithm
        val decoded = decodePolyline("_p~iF~ps|U_ulLnnqC_mqNvxq`@")

        assertEquals(3, decoded.size)
        assertLatLonEquals(38.5, -120.2, decoded[0])
        assertLatLonEquals(40.7, -120.95, decoded[1])
        assertLatLonEquals(43.252, -126.453, decoded[2])
    }

    @Test
    fun `empty string decodes to no points`() {
        assertEquals(emptyList(), decodePolyline(""))
    }

    private fun assertLatLonEquals(expectedLat: Double, expectedLon: Double, actual: dev.garado.transit.map.LatLon) {
        assertEquals(expectedLat, actual.lat, absoluteTolerance = 1e-5)
        assertEquals(expectedLon, actual.lon, absoluteTolerance = 1e-5)
    }
}
