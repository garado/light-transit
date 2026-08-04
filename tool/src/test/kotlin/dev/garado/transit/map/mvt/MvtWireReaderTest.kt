package dev.garado.transit.map.mvt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MvtWireReaderTest {

    @Test
    fun `readVarint decodes single-byte value`() {
        val reader = MvtWireReader(byteArrayOf(0x01))
        assertEquals(1L, reader.readVarint())
    }

    @Test
    fun `readVarint decodes multi-byte value`() {
        // 300 = 0b1_0010_1100 -> low 7 bits 0101100 with continuation, then 0000010
        val reader = MvtWireReader(byteArrayOf(0xAC.toByte(), 0x02))
        assertEquals(300L, reader.readVarint())
    }

    @Test
    fun `readVarint throws on truncated input`() {
        // continuation bit set but no following byte
        val reader = MvtWireReader(byteArrayOf(0x80.toByte()))
        assertFailsWith<IllegalStateException> { reader.readVarint() }
    }

    @Test
    fun `readVarint throws when longer than 64 bits`() {
        // 10 bytes, each with the continuation bit set
        val bytes = ByteArray(10) { 0x80.toByte() }
        val reader = MvtWireReader(bytes)
        assertFailsWith<IllegalStateException> { reader.readVarint() }
    }

    @Test
    fun `readZigZag round-trips small signed values`() {
        // zigzag encoding: 0->0, -1->1, 1->2, -2->3, 2->4
        val cases = mapOf(
            0x00 to 0L,
            0x01 to -1L,
            0x02 to 1L,
            0x03 to -2L,
            0x04 to 2L,
        )
        for ((encoded, expected) in cases) {
            val reader = MvtWireReader(byteArrayOf(encoded.toByte()))
            assertEquals(expected, reader.readZigZag())
        }
    }

    @Test
    fun `readTag splits field number and wire type`() {
        // field 3, wire type 2 (length-delimited) -> tag = (3 shl 3) or 2 = 26
        val reader = MvtWireReader(byteArrayOf(26))
        val (fieldNumber, wireType) = reader.readTag()
        assertEquals(3, fieldNumber)
        assertEquals(2, wireType)
    }

    @Test
    fun `readLengthDelimited reads the declared number of bytes`() {
        val reader = MvtWireReader(byteArrayOf(0x03, 0x0A, 0x0B, 0x0C, 0x0D))
        val slice = reader.readLengthDelimited()
        assertEquals(listOf<Byte>(0x0A, 0x0B, 0x0C), slice.toList())
        // cursor should now sit at the byte after the slice
        assertEquals(0x0D.toLong(), reader.readVarint())
    }

    @Test
    fun `readLengthDelimited throws when length exceeds remaining bytes`() {
        val reader = MvtWireReader(byteArrayOf(0x05, 0x0A))
        assertFailsWith<IllegalStateException> { reader.readLengthDelimited() }
    }

    @Test
    fun `skipField advances past a varint field`() {
        val reader = MvtWireReader(byteArrayOf(0xAC.toByte(), 0x02, 0x07))
        reader.skipField(0)
        assertEquals(7L, reader.readVarint())
    }

    @Test
    fun `skipField advances past a 64-bit field`() {
        val reader = MvtWireReader(ByteArray(8) { 0x00 } + byteArrayOf(0x07))
        reader.skipField(1)
        assertEquals(7L, reader.readVarint())
    }

    @Test
    fun `skipField advances past a length-delimited field`() {
        val reader = MvtWireReader(byteArrayOf(0x02, 0xAA.toByte(), 0xBB.toByte(), 0x07))
        reader.skipField(2)
        assertEquals(7L, reader.readVarint())
    }

    @Test
    fun `skipField advances past a 32-bit field`() {
        val reader = MvtWireReader(ByteArray(4) { 0x00 } + byteArrayOf(0x07))
        reader.skipField(5)
        assertEquals(7L, reader.readVarint())
    }

    @Test
    fun `skipField throws on unsupported wire type`() {
        val reader = MvtWireReader(byteArrayOf(0x00))
        assertFailsWith<IllegalArgumentException> { reader.skipField(3) }
    }

    @Test
    fun `hasNext reflects remaining bytes`() {
        val reader = MvtWireReader(byteArrayOf(0x01))
        assertEquals(true, reader.hasNext())
        reader.readVarint()
        assertEquals(false, reader.hasNext())
    }
}
