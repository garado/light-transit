package dev.garado.transit.gtfs.local

import java.io.BufferedReader
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GtfsStopsTxtParserTest {

    @Test
    fun `parses a well-formed row`() {
        val stops = parse(
            """
            stop_id,stop_name,stop_lat,stop_lon
            1,Main St,40.7128,-74.0060
            """.trimIndent()
        )

        assertEquals(1, stops.size)
        assertEquals("1", stops[0].stopId)
        assertEquals("Main St", stops[0].name)
        assertEquals(40.7128, stops[0].lat)
        assertEquals(-74.0060, stops[0].lon)
    }

    @Test
    fun `quoted field with an embedded comma is kept as one field`() {
        val stops = parse(
            """
            stop_id,stop_name,stop_lat,stop_lon
            1,"123 Main St, Suite 4",40.7128,-74.0060
            """.trimIndent()
        )

        assertEquals(1, stops.size)
        assertEquals("123 Main St, Suite 4", stops[0].name)
    }

    @Test
    fun `escaped double quote inside a quoted field is unescaped`() {
        val stops = parse(
            """
            stop_id,stop_name,stop_lat,stop_lon
            1,"The ""Downtown"" Stop",40.7128,-74.0060
            """.trimIndent()
        )

        assertEquals(1, stops.size)
        assertEquals("""The "Downtown" Stop""", stops[0].name)
    }

    @Test
    fun `missing a required column returns an empty list`() {
        val stops = parse(
            """
            stop_id,stop_name,stop_lon
            1,Main St,-74.0060
            """.trimIndent()
        )

        assertTrue(stops.isEmpty())
    }

    @Test
    fun `row with non-numeric lat is skipped but later rows still parse`() {
        val stops = parse(
            """
            stop_id,stop_name,stop_lat,stop_lon
            1,Bad Row,not-a-number,-74.0060
            2,Good Row,40.7128,-74.0060
            """.trimIndent()
        )

        assertEquals(1, stops.size)
        assertEquals("2", stops[0].stopId)
    }

    @Test
    fun `row with blank stop_id is skipped`() {
        val stops = parse(
            """
            stop_id,stop_name,stop_lat,stop_lon
            ,Main St,40.7128,-74.0060
            """.trimIndent()
        )

        assertTrue(stops.isEmpty())
    }

    @Test
    fun `blank lines between rows are ignored`() {
        val stops = parse(
            """
            stop_id,stop_name,stop_lat,stop_lon

            1,Main St,40.7128,-74.0060

            """.trimIndent()
        )

        assertEquals(1, stops.size)
    }

    @Test
    fun `empty input returns an empty list`() {
        assertTrue(parse("").isEmpty())
    }

    @Test
    fun `location_type is stored as-is, including blank as null`() {
        val stops = parse(
            """
            stop_id,stop_name,stop_lat,stop_lon,location_type
            1,Platform,40.7128,-74.0060,0
            2,No Type,40.7128,-74.0060,
            3,Elevator,40.7128,-74.0060,2
            """.trimIndent()
        )

        assertEquals(3, stops.size)
        assertEquals("0", stops.single { it.stopId == "1" }.locationType)
        assertEquals(null, stops.single { it.stopId == "2" }.locationType)
        assertEquals("2", stops.single { it.stopId == "3" }.locationType)
    }

    private fun parse(csv: String): List<GtfsStopEntity> =
        GtfsStopsTxtParser.parse(sourceId = 1L, reader = BufferedReader(StringReader(csv)))
}
