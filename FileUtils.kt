package com.edm.downloadmanager.util

import com.edm.downloadmanager.data.db.FileCategory
import java.io.File

object FileUtils {

    private val videoExt = setOf("mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "m4v")
    private val audioExt = setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "wma")
    private val imageExt = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic")
    private val docExt = setOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv")
    private val archiveExt = setOf("zip", "rar", "7z", "tar", "gz", "bz2")

    fun categorize(fileName: String): FileCategory {
        val ext = fileName.substringAfterLast('.', "").lowercase()
        return when {
            ext == "apk" -> FileCategory.APK
            ext == "pdf" -> FileCategory.DOCUMENT
            ext == "iso" -> FileCategory.ISO
            ext in videoExt -> FileCategory.VIDEO
            ext in audioExt -> FileCategory.AUDIO
            ext in imageExt -> FileCategory.IMAGE
            ext in docExt -> FileCategory.DOCUMENT
            ext in archiveExt -> FileCategory.ARCHIVE
            else -> FileCategory.OTHER
        }
    }

    /** Automatic folder organization: e.g. /EDM/Videos, /EDM/Documents, /EDM/APKs */
    fun subfolderFor(category: FileCategory): String = when (category) {
        FileCategory.VIDEO -> "Videos"
        FileCategory.AUDIO -> "Audio"
        FileCategory.IMAGE -> "Images"
        FileCategory.DOCUMENT -> "Documents"
        FileCategory.APK -> "APKs"
        FileCategory.ARCHIVE -> "Archives"
        FileCategory.ISO -> "ISOs"
        FileCategory.OTHER -> "Other"
    }

    /** Strips illegal characters and truncates absurdly long names pulled from URLs. */
    fun sanitizeFileName(raw: String): String {
        var name = raw.substringBefore('?').substringBefore('#').trim()
        if (name.isBlank()) name = "download_${System.currentTimeMillis()}"
        name = name.replace(Regex("[/\\\\:*?\"<>|]"), "_")
        return if (name.length > 180) name.takeLast(180) else name
    }

    /** Ensures no existing file is overwritten: file.pdf, file (1).pdf, file (2).pdf ... */
    fun resolveNonConflictingFile(baseDir: File, fileName: String): File {
        baseDir.mkdirs()
        var candidate = File(baseDir, fileName)
        if (!candidate.exists()) return candidate
        val dot = fileName.lastIndexOf('.')
        val base = if (dot > 0) fileName.substring(0, dot) else fileName
        val ext = if (dot > 0) fileName.substring(dot) else ""
        var i = 1
        while (candidate.exists()) {
            candidate = File(baseDir, "$base ($i)$ext")
            i++
        }
        return candidate
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val units = arrayOf("KB", "MB", "GB", "TB")
        var value = bytes / 1024.0
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return "%.2f %s".format(value, units[unitIndex])
    }

    fun formatSpeed(bytesPerSec: Long): String = "${formatBytes(bytesPerSec)}/s"

    fun formatEta(remainingBytes: Long, bytesPerSec: Long): String {
        if (bytesPerSec <= 0) return "--:--"
        val seconds = remainingBytes / bytesPerSec
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }
}
