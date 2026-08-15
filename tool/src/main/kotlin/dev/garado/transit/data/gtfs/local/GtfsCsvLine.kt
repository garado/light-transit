package dev.garado.transit.data.gtfs.local

/** RFC4180 split (handle quoted fields with embedded commas/escaped quotes) for GTFS txt parsers */
internal fun splitGtfsCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                current.append('"')
                i++
            }
            c == '"' -> inQuotes = !inQuotes
            c == ',' && !inQuotes -> {
                fields.add(current.toString().trim())
                current.clear()
            }
            else -> current.append(c)
        }
        i++
    }
    fields.add(current.toString().trim())
    return fields
}
