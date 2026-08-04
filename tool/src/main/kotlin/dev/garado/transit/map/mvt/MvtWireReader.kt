/**
 * Generic protobuf wire-format primitives
 */

package dev.garado.transit.map.mvt

/**
 * Cursor-based reader over raw protobuf wire-format bytes (single-pass, forward-only, not thread-safe)
 */
class MvtWireReader(private val bytes: ByteArray) {
    private var pos = 0

    fun hasNext(): Boolean = pos < bytes.size

    /** Reads a protobuf varint (little-endian base-128, high bit = continuation) */
    fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            if (pos >= bytes.size) {
                throw IllegalStateException("Truncated varint at end of input")
            }
            if (shift >= 64) {
                throw IllegalStateException("Varint too long (exceeds 64 bits)")
            }
            val b = bytes[pos].toInt() and 0xFF
            pos++
            result = result or ((b.toLong() and 0x7F) shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        return result
    }

    /** Reads a varint and decodes it as a zigzag-encoded signed integer (protobuf sint32/sint64) */
    fun readZigZag(): Long {
        val n = readVarint()
        return (n ushr 1) xor -(n and 1)
    }

    /** Reads a tag varint and splits it into (fieldNumber, wireType) */
    fun readTag(): Pair<Int, Int> {
        val tag = readVarint()
        val fieldNumber = (tag shr 3).toInt()
        val wireType = (tag and 0x7).toInt()
        return fieldNumber to wireType
    }

    /** Reads a length-delimited field (wire type 2): a varint length followed by that many bytes */
    fun readLengthDelimited(): ByteArray {
        val length = readVarint()
        if (length < 0 || length > bytes.size - pos) {
            throw IllegalStateException("Invalid length-delimited field size: $length")
        }
        val end = pos + length.toInt()
        val slice = bytes.copyOfRange(pos, end)
        pos = end
        return slice
    }

    /** Skips a field's value given its wire type. Only covers wire types MVT actually emits. */
    fun skipField(wireType: Int) {
        when (wireType) {
            0 -> readVarint()
            1 -> pos += 8
            2 -> readLengthDelimited()
            5 -> pos += 4
            else -> throw IllegalArgumentException("Unsupported wire type: $wireType")
        }
    }
}
