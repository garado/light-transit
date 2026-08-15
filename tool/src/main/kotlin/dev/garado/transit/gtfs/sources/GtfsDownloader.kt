package dev.garado.transit.gtfs.sources

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "GtfsDownloader"

/** Downloads a GTFS .zip and writes it to local storage */
internal class GtfsDownloader {
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            connectTimeoutMillis = 15_000L
            requestTimeoutMillis = 120_000L
        }
    }

    /** Streams response to disk and reports download progress back to caller */
    suspend fun download(url: String, destination: File, onProgress: (Int) -> Unit = {}): Boolean =
        withContext(Dispatchers.IO) {
            try {
                destination.parentFile?.mkdirs()
                client.prepareGet(url) {
                    onDownload { bytesSentTotal, contentLength ->
                        if (contentLength != null && contentLength > 0) {
                            onProgress(((bytesSentTotal * 100) / contentLength).toInt())
                        }
                    }
                }.execute { response ->
                    destination.outputStream().use { output -> response.bodyAsChannel().copyTo(output) }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download $url", e)
                destination.delete()
                false
            }
        }

    companion object {
        val shared by lazy { GtfsDownloader() }
    }
}
