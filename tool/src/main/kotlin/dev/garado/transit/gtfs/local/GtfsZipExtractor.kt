package dev.garado.transit.gtfs.local

import java.io.File
import java.util.zip.ZipFile

/** Reads a single named entry out of a GTFS zip */
internal object GtfsZipExtractor {
    fun readEntryText(zipFile: File, entryName: String): String? = try {
        ZipFile(zipFile).use { zip ->
            val entry = zip.entries().asSequence().firstOrNull { it.name.substringAfterLast('/') == entryName }
            entry?.let { zip.getInputStream(it).use { stream -> stream.readBytes().decodeToString() } }
        }
    } catch (e: Exception) {
        null
    }
}
