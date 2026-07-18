package com.edm.downloadmanager.engine

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class UrlAnalysis(
    val finalUrl: String,
    val contentLength: Long,      // -1 if unknown
    val supportsRange: Boolean,
    val mimeType: String?,
    val suggestedFileName: String,
    val eTag: String?
)

/**
 * Built-in URL analyzer: issues a HEAD (falling back to a ranged GET) request
 * to discover file size, MIME type, filename and whether the server honors
 * HTTP Range requests -- the prerequisite for segmented multi-thread downloads.
 */
object RangeSupportChecker {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun analyze(url: String): UrlAnalysis {
        // Try HEAD first; some servers reject HEAD, so fall back to a 1-byte ranged GET.
        val headRequest = Request.Builder().url(url).head().build()
        client.newCall(headRequest).execute().use { response ->
            if (response.isSuccessful || response.code in 200..399) {
                val length = response.header("Content-Length")?.toLongOrNull() ?: -1L
                val acceptRanges = response.header("Accept-Ranges")
                val supportsRange = acceptRanges?.contains("bytes", ignoreCase = true) == true
                return UrlAnalysis(
                    finalUrl = response.request.url.toString(),
                    contentLength = length,
                    supportsRange = supportsRange,
                    mimeType = response.header("Content-Type"),
                    suggestedFileName = extractFileName(response.request.url.toString(), response.header("Content-Disposition")),
                    eTag = response.header("ETag")
                )
            }
        }

        val rangeProbe = Request.Builder().url(url).header("Range", "bytes=0-0").build()
        client.newCall(rangeProbe).execute().use { response ->
            val supportsRange = response.code == 206
            val contentRange = response.header("Content-Range") // e.g. bytes 0-0/12345
            val total = contentRange?.substringAfterLast('/')?.toLongOrNull()
                ?: response.header("Content-Length")?.toLongOrNull() ?: -1L
            return UrlAnalysis(
                finalUrl = response.request.url.toString(),
                contentLength = total,
                supportsRange = supportsRange,
                mimeType = response.header("Content-Type"),
                suggestedFileName = extractFileName(response.request.url.toString(), response.header("Content-Disposition")),
                eTag = response.header("ETag")
            )
        }
    }

    private fun extractFileName(url: String, contentDisposition: String?): String {
        contentDisposition?.let {
            val match = Regex("filename\\*?=\"?([^\";]+)\"?").find(it)
            if (match != null) return match.groupValues[1].substringAfterLast('/')
        }
        val fromUrl = url.substringBefore('?').substringAfterLast('/')
        return if (fromUrl.isNotBlank()) fromUrl else "download_${System.currentTimeMillis()}"
    }
}
