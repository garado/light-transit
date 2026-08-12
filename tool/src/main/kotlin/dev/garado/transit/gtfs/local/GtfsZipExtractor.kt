package dev.garado.transit.gtfs.local

import android.util.Log
import java.io.File
import java.util.zip.ZipFile

private const val TAG = "GtfsZipExtractor"

/** Reads a single named entry out of a GTFS zip */
internal object GtfsZipExtractor {
    fun readEntryText(zipFile: File, entryName: String): String? = try {
        ZipFile(zipFile).use { zip ->
            val names = zip.entries().asSequence().map { it.name }.toList()
            val entry = zip.entries().asSequence().firstOrNull { it.name.substringAfterLast('/') == entryName }
            if (entry == null) {
                Log.w(TAG, "$entryName not found; zip contains: ${names.take(20)}")
            }
            entry?.let { zip.getInputStream(it).use { stream -> stream.readBytes().decodeToString() } }
        }
    } catch (e: Exception) {
        Log.e(TAG, "failed to read $entryName from ${zipFile.name}", e)
        null
    }
}
