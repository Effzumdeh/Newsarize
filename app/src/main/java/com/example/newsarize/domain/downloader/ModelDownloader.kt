package com.example.newsarize.domain.downloader

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int, val downloadedMb: Float, val totalMb: Float) : DownloadState()
    object Processing : DownloadState()
    object Finished : DownloadState()
    data class Error(val message: String) : DownloadState()
}

class ModelDownloader(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    fun getModelDirectory(): File {
        val dir = File(context.filesDir, "models")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun isModelDownloaded(fileName: String): Boolean {
        val file = File(getModelDirectory(), fileName)
        return file.exists() && file.length() > 0
    }

    fun downloadModel(url: String, fileName: String): Long {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading AI Model ($fileName)")
            .setDescription("Downloading ONNX Model for local inference.")
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)

        return downloadManager.enqueue(request)
    }

    fun getDownloadProgressFlow(downloadId: Long, fileName: String): Flow<DownloadState> = flow {
        var isDownloading = true
        while (isDownloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = downloadManager.query(query)
            if (cursor != null && cursor.moveToFirst()) {
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                val status = if (statusIndex != -1) cursor.getInt(statusIndex) else DownloadManager.STATUS_FAILED

                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                
                val bytesDownloaded = if (bytesDownloadedIndex != -1) cursor.getLong(bytesDownloadedIndex) else 0L
                val bytesTotal = if (bytesTotalIndex != -1) cursor.getLong(bytesTotalIndex) else 0L

                if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PENDING) {
                    val progress = if (bytesTotal > 0) ((bytesDownloaded * 100L) / bytesTotal).toInt() else 0
                    val downMb = bytesDownloaded / (1024f * 1024f)
                    val totMb = if (bytesTotal > 0) bytesTotal / (1024f * 1024f) else 0f
                    emit(DownloadState.Downloading(progress, downMb, totMb))
                }

                if (status == DownloadManager.STATUS_SUCCESSFUL || status == DownloadManager.STATUS_FAILED) {
                    isDownloading = false
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        emit(DownloadState.Processing)
                        try {
                            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val uriString = if (uriIndex != -1) cursor.getString(uriIndex) else null
                            
                            if (uriString != null) {
                                val path = Uri.parse(uriString).path
                                if (path != null) {
                                    val downloadedFile = File(path)
                                    val destFile = File(getModelDirectory(), fileName)
                                    downloadedFile.copyTo(destFile, overwrite = true)
                                    // Clean up external file
                                    downloadedFile.delete()
                                    emit(DownloadState.Finished)
                                } else {
                                    emit(DownloadState.Error("Dateipfad vom System-URI-Parser nicht extrahierbar."))
                                }
                            } else {
                                emit(DownloadState.Error("Download URI vom System nicht bereitgestellt."))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            emit(DownloadState.Error("Dateikopierfehler: ${e.message}"))
                        }
                    } else {
                        val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = if (reasonIndex != -1) cursor.getInt(reasonIndex) else -1
                        emit(DownloadState.Error("Download fehlgeschlagen (Fehlercode: $reason)"))
                    }
                }
            } else {
                 isDownloading = false
                 emit(DownloadState.Error("Download wurde abgebrochen oder nicht gefunden."))
            }
            cursor?.close()
            if (isDownloading) {
                delay(500)
            }
        }
    }
}
