package dev.garado.transit.data.gtfs.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GeohashTest {

    @Test
    fun `encodes a known reference point`() {
        // https://en.wikipedia.org/wiki/Geohash reference example
        assertEquals("u4pruy", Geohash.encode(57.64911, 10.40744, precision = 6))
    }

    @Test
    fun `higher precision produces a prefix-compatible longer hash`() {
        val short = Geohash.encode(57.64911, 10.40744, precision = 4)
        val long = Geohash.encode(57.64911, 10.40744, precision = 8)

        assertTrue(long.startsWith(short))
    }

    @Test
    fun `negative lat and lon encode without error`() {
        val hash = Geohash.encode(-33.8688, 151.2093, precision = 6)

        assertEquals(6, hash.length)
    }

    @Test
    fun `cellsCovering a point-sized box returns exactly one cell`() {
        val cells = Geohash.cellsCovering(minLat = 1.0, maxLat = 1.0, minLon = 1.0, maxLon = 1.0, precision = 6)

        assertEquals(1, cells.size)
        assertEquals(Geohash.encode(1.0, 1.0, 6), cells.single())
    }

    @Test
    fun `cellsCovering a box straddling a cell boundary returns multiple cells`() {
        // Precision 1 cells are ~5000km wide, so a box spanning a hemisphere crosses several.
        val cells = Geohash.cellsCovering(minLat = -10.0, maxLat = 10.0, minLon = -10.0, maxLon = 10.0, precision = 1)

        assertTrue(cells.size > 1)
    }

    @Test
    fun `cellsCovering near the north pole does not throw or return empty`() {
        val cells = Geohash.cellsCovering(minLat = 89.0, maxLat = 90.0, minLon = -180.0, maxLon = 180.0, precision = 4)

        assertTrue(cells.isNotEmpty())
    }

    @Test
    fun `cellsCovering near the antimeridian does not throw or return empty`() {
        val cells = Geohash.cellsCovering(minLat = -1.0, maxLat = 1.0, minLon = 179.0, maxLon = 180.0, precision = 5)

        assertTrue(cells.isNotEmpty())
    }

    @Test
    fun `cellsCovering always includes the cell each corner encodes to`() {
        val minLat = 40.0
        val maxLat = 41.0
        val minLon = -74.0
        val maxLon = -73.0
        val precision = 5

        val cells = Geohash.cellsCovering(minLat, maxLat, minLon, maxLon, precision).toSet()

        assertTrue(Geohash.encode(minLat, minLon, precision) in cells)
        assertTrue(Geohash.encode(maxLat, maxLon, precision) in cells)
    }
}
