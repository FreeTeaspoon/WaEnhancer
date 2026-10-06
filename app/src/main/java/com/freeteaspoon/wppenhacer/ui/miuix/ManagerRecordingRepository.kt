package com.freeteaspoon.wppenhacer.ui.miuix

import android.os.Environment
import android.media.MediaMetadataRetriever
import java.io.File

internal data class ManagerRecording(
    val file: File,
    val contactName: String,
    val duration: Long,
    val date: Long,
    val size: Long,
) {
    val formattedDuration: String get() {
        val totalSeconds = duration / 1000
        return if (totalSeconds >= 3600) "%d:%02d:%02d".format(totalSeconds / 3600, totalSeconds / 60 % 60, totalSeconds % 60)
        else "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    }
    val formattedSize: String get() = when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "%.1f KB".format(size / 1024.0)
        else -> "%.1f MB".format(size / (1024.0 * 1024.0))
    }
}

private val recordingNamePattern = Regex("Call_([+\\w\\s]+)_\\d{8}_\\d{6}\\.(wav|m4a|mp3|aac)", RegexOption.IGNORE_CASE)

internal fun scanRecordings(configuredPath: String?, unknownContact: String): List<ManagerRecording> {
    val roots = linkedSetOf(
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "WA Call Recordings"),
        File(Environment.getExternalStorageDirectory(), "WA Call Recordings"),
        File(Environment.getExternalStorageDirectory(), "Android/data/com.whatsapp/files/Recordings"),
        File(Environment.getExternalStorageDirectory(), "Android/data/com.whatsapp.w4b/files/Recordings"),
        File(Environment.getExternalStorageDirectory(), "Music/WaEnhancer/Recordings"),
    )
    configuredPath?.takeIf(String::isNotBlank)?.let { roots += File(it, "WA Call Recordings") }
    return roots.flatMap { root -> root.walkTopDown().filter { it.isFile && it.extension.lowercase() in setOf("wav", "mp3", "aac", "m4a") }.toList() }
        .distinctBy { runCatching { it.canonicalPath }.getOrDefault(it.absolutePath) }
        .map { file ->
            val contact = recordingNamePattern.matchEntire(file.name)?.groupValues?.getOrNull(1)?.takeIf(String::isNotBlank) ?: unknownContact
            val duration = runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(file.absolutePath)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                }
            }.getOrDefault(0L)
            ManagerRecording(file, contact, duration, file.lastModified(), file.length())
        }.sortedByDescending { it.date }
}
