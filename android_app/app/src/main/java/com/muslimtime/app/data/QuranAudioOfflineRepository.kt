package com.muslimtime.app.data

import android.content.Context
import android.net.Uri
import android.text.format.Formatter
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Collections

object QuranAudioOfflineRepository {
    private const val ROOT_DIR = "quran_audio_offline"
    private const val MANIFEST_FILE = "manifest.json"
    private val downloadingSuras = Collections.synchronizedSet(mutableSetOf<Int>())

    data class SuraOfflineStatus(
        val isDownloaded: Boolean,
        val totalBytes: Long,
        val fileCount: Int,
    )

    fun isDownloading(suraNumber: Int): Boolean = downloadingSuras.contains(suraNumber)

    fun status(context: Context, suraNumber: Int): SuraOfflineStatus {
        val manifest = loadManifest(context, suraNumber) ?: return SuraOfflineStatus(false, 0L, 0)
        val entries = manifest.optJSONArray("entries") ?: return SuraOfflineStatus(false, 0L, 0)
        var totalBytes = 0L
        var validFiles = 0
        for (index in 0 until entries.length()) {
            val entry = entries.getJSONObject(index)
            val file = File(suraDir(context, suraNumber), entry.getString("fileName"))
            if (!file.exists()) {
                return SuraOfflineStatus(false, 0L, 0)
            }
            totalBytes += file.length()
            validFiles += 1
        }
        return SuraOfflineStatus(validFiles > 0, totalBytes, validFiles)
    }

    fun resolvePlaybackAyahs(context: Context, suraNumber: Int, ayahs: List<AudioAyahItem>): List<AudioAyahItem> {
        val manifest = loadManifest(context, suraNumber) ?: return ayahs
        val entries = manifest.optJSONArray("entries") ?: return ayahs
        val localMap = buildMap {
            for (index in 0 until entries.length()) {
                val entry = entries.getJSONObject(index)
                val file = File(suraDir(context, suraNumber), entry.getString("fileName"))
                if (file.exists()) {
                    put(entry.getString("ayahKey"), Uri.fromFile(file).toString())
                }
            }
        }
        if (localMap.isEmpty()) return ayahs
        return ayahs.map { ayah ->
            ayah.copy(audioUrl = localMap[ayah.ayahKey] ?: ayah.audioUrl)
        }
    }

    fun downloadSura(
        context: Context,
        suraNumber: Int,
        ayahs: List<AudioAyahItem>,
        onProgress: ((downloadedBytes: Long, totalBytes: Long) -> Unit)? = null,
    ): Result<SuraOfflineStatus> {
        return runCatching {
            check(ayahs.isNotEmpty()) { "No audio ayahs available to download." }
            check(downloadingSuras.add(suraNumber)) { "Download already in progress." }

            val dir = suraDir(context, suraNumber)
            dir.deleteRecursively()
            dir.mkdirs()

            val manifestEntries = JSONArray()
            var downloadedBytes = 0L
            var totalBytes = 0L
            try {
                ayahs.forEach { ayah ->
                    val fileName = buildFileName(ayah)
                    val targetFile = File(dir, fileName)
                    if (ayah.audioUrl.startsWith("http://") || ayah.audioUrl.startsWith("https://")) {
                        val transfer = downloadFile(ayah.audioUrl, targetFile) { copied, expected ->
                            val resolvedTotal = (totalBytes + expected).coerceAtLeast(downloadedBytes + copied)
                            onProgress?.invoke(downloadedBytes + copied, resolvedTotal)
                        }
                        totalBytes += transfer.expectedBytes
                        downloadedBytes += transfer.downloadedBytes
                    } else {
                        copyLocalUri(context, ayah.audioUrl, targetFile)
                        downloadedBytes += targetFile.length()
                        totalBytes += targetFile.length()
                        onProgress?.invoke(downloadedBytes, totalBytes)
                    }
                    manifestEntries.put(
                        JSONObject().apply {
                            put("ayahKey", ayah.ayahKey)
                            put("remoteUrl", ayah.audioUrl)
                            put("fileName", fileName)
                            put("sizeBytes", targetFile.length())
                        },
                    )
                }
                File(dir, MANIFEST_FILE).writeText(
                    JSONObject().apply {
                        put("suraNumber", suraNumber)
                        put("entries", manifestEntries)
                    }.toString(),
                )
            } catch (error: Throwable) {
                dir.deleteRecursively()
                throw error
            } finally {
                downloadingSuras.remove(suraNumber)
            }

            status(context, suraNumber)
        }
    }

    fun deleteSura(context: Context, suraNumber: Int): Boolean {
        downloadingSuras.remove(suraNumber)
        return suraDir(context, suraNumber).deleteRecursively()
    }

    fun deleteAll(context: Context): Boolean {
        downloadingSuras.clear()
        return rootDir(context).deleteRecursively()
    }

    fun downloadedSuraNumbers(context: Context): Set<Int> {
        val root = rootDir(context)
        if (!root.exists()) return emptySet()
        return root.listFiles()
            ?.mapNotNull { dir ->
                if (!dir.isDirectory || !dir.name.startsWith("sura_")) return@mapNotNull null
                dir.name.removePrefix("sura_").toIntOrNull()
                    ?.takeIf { status(context, it).isDownloaded }
            }
            ?.toSet()
            ?: emptySet()
    }

    fun totalDownloadedBytes(context: Context): Long {
        val root = rootDir(context)
        if (!root.exists()) return 0L
        return root.walkTopDown()
            .filter { it.isFile && it.name != MANIFEST_FILE }
            .sumOf { it.length() }
    }

    fun formatSize(context: Context, bytes: Long): String = Formatter.formatShortFileSize(context, bytes.coerceAtLeast(0))

    private fun loadManifest(context: Context, suraNumber: Int): JSONObject? {
        val manifestFile = File(suraDir(context, suraNumber), MANIFEST_FILE)
        if (!manifestFile.exists()) return null
        return runCatching { JSONObject(manifestFile.readText()) }.getOrNull()
    }

    private fun rootDir(context: Context): File = File(context.filesDir, ROOT_DIR).apply { mkdirs() }

    private fun suraDir(context: Context, suraNumber: Int): File = File(rootDir(context), "sura_$suraNumber")

    private fun buildFileName(ayah: AudioAyahItem): String {
        val extension = runCatching {
            val path = URL(ayah.audioUrl).path
            path.substringAfterLast('.', "mp3").ifBlank { "mp3" }
        }.getOrDefault("mp3")
        return ayah.ayahKey.replace(':', '_') + "." + extension
    }

    private fun downloadFile(
        url: String,
        targetFile: File,
        onProgress: ((copiedBytes: Long, expectedBytes: Long) -> Unit)? = null,
    ): DownloadTransfer {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 20000
            readTimeout = 20000
        }
        val expectedBytes = connection.contentLengthLong.coerceAtLeast(0L)
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream ?: error("Audio download HTTP ${connection.responseCode}")
        }
        var copiedBytes = 0L
        targetFile.outputStream().use { output ->
            stream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    output.write(buffer, 0, read)
                    copiedBytes += read
                    onProgress?.invoke(copiedBytes, expectedBytes)
                }
            }
        }
        return DownloadTransfer(downloadedBytes = copiedBytes, expectedBytes = expectedBytes.takeIf { it > 0 } ?: copiedBytes)
    }

    private fun copyLocalUri(context: Context, uriString: String, targetFile: File) {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(targetFile).use { output -> input.copyTo(output) }
        } ?: error("Unable to read local audio URI.")
    }

    private data class DownloadTransfer(
        val downloadedBytes: Long,
        val expectedBytes: Long,
    )
}
