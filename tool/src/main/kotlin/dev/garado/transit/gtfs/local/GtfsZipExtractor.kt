package dev.garado.transit.gtfs.local

import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

private const val TAG = "GtfsZipExtractor"

/**
 * Streams a single named entry out of a GTFS zip to [block].
 *
 * Uses ZipInputStream (reads local file headers sequentially) rather than ZipFile (reads the
 * central directory). Some GTFS datasets have duplicate entry names causing ZipFile to fail -
 * it failed on SF Bay Area bc it had duplicate Attribution entry.
 */
internal object GtfsZipExtractor {
    fun <T> readEntry(zipFile: File, entryName: String, block: (BufferedReader) -> T): T? = try {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name.substringAfterLast('/') == entryName) {
                    return@use block(BufferedReader(InputStreamReader(zip)))
                }
                entry = zip.nextEntry
            }
            Log.w(TAG, "$entryName not found in ${zipFile.name}")
            null
        }
    } catch (e: Exception) {
        Log.e(TAG, "failed to read $entryName from ${zipFile.name}", e)
        null
    }
}
