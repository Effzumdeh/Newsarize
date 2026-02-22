package com.example.newsarize.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsarize.data.local.AppDatabase
import com.example.newsarize.data.local.entity.ArticleEntity
import com.example.newsarize.data.local.entity.FeedSourceEntity
import com.example.newsarize.data.network.RssService
import com.example.newsarize.domain.ai.MediaPipeAiService
import com.example.newsarize.domain.downloader.ModelDownloader
import com.example.newsarize.domain.downloader.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.net.Uri
import android.provider.OpenableColumns
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.util.zip.GZIPInputStream

class NewsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val rssService = RssService()
    private val downloader = ModelDownloader(application)
    private val aiService = MediaPipeAiService(application)

    private val _isModelReady = MutableStateFlow(false)
    val isModelReady = _isModelReady.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing = _isSummarizing.asStateFlow()

    val articles = db.articleDao().getAllArticlesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val feedSources = db.feedSourceDao().getAllFeedSourcesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        checkModelStatus()
        // Prefill default feeds if empty
        viewModelScope.launch {
            if (db.feedSourceDao().getAllFeedSources().isEmpty()) {
                db.feedSourceDao().insertFeedSource(FeedSourceEntity(name = "Tagesschau", url = "https://www.tagesschau.de/xml/rss2"))
                db.feedSourceDao().insertFeedSource(FeedSourceEntity(name = "Heise", url = "https://www.heise.de/rss/heise-atom.xml"))
            }
        }
        
        // Start infinite inference worker
        startInferenceWorker()
    }

    fun checkModelStatus() {
        val modelDir = downloader.getModelDirectory()
        val binFiles = modelDir.listFiles { _, name -> name.endsWith(".bin") }
        if (binFiles != null && binFiles.isNotEmpty()) {
            val fileModel = binFiles[0] // take the first valid model
            
            // Check file size (Gemma 2B should be ~1.2 GB up to ~2.6 GB for INT8)
            if (fileModel.length() < 100 * 1024 * 1024) {
                fileModel.delete()
                _isModelReady.value = false
                _downloadState.value = DownloadState.Error("MediaPipe-Modell war fehlerhaft (zu klein).")
                return
            }

            // Init AI Service
            viewModelScope.launch {
                try {
                    val success = aiService.initialize(fileModel)
                    if (success) {
                        _isModelReady.value = true
                        _downloadState.value = DownloadState.Finished
                    }
                } catch (e: Exception) {
                    _isModelReady.value = false
                    _downloadState.value = DownloadState.Error("Init-Fehler: ${e.message}")
                }
            }
        } else {
            _isModelReady.value = false
            _downloadState.value = DownloadState.Error(
                "Kein Modell gefunden. \nBitte lade 'gemma-2b-it-gpu-int4' oder '...int8' als .tar.gz oder .bin herunter " +
                "und wähle die Datei zum Importieren aus."
            )
        }
    }

    fun importModelFromUri(uri: Uri) {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Downloading(0, 0f, 0f)
            
            try {
                val contentResolver = getApplication<Application>().contentResolver
                val inputStream: InputStream? = contentResolver.openInputStream(uri)
                
                if (inputStream == null) {
                    _downloadState.value = DownloadState.Error("Datei konnte nicht gelesen werden.")
                    return@launch
                }
                
                // Get file name and size metadata
                var fileName = ""
                var totalBytesRaw = 0L
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = it.getString(nameIndex)
                        }
                        
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeIndex != -1) {
                            totalBytesRaw = it.getLong(sizeIndex)
                        }
                    }
                }
                
                val outputDir = downloader.getModelDirectory()
                if (!outputDir.exists()) outputDir.mkdirs()

                // Clean old bin files
                outputDir.listFiles { _, name -> name.endsWith(".bin") }?.forEach { it.delete() }
                
                val isTarGz = fileName.lowercase().endsWith(".tar.gz")
                
                if (isTarGz) {
                    inputStream.use { input ->
                        GZIPInputStream(input).use { gzipIn ->
                            TarArchiveInputStream(gzipIn).use { tarIn ->
                                var entry = tarIn.nextTarEntry
                                var foundBin = false
                                while (entry != null) {
                                    if (!entry.isDirectory && entry.name.endsWith(".bin")) {
                                        foundBin = true
                                        val destFile = File(outputDir, File(entry.name).name)
                                        val fos = FileOutputStream(destFile)
                                        
                                        val buffer = ByteArray(16 * 1024)
                                        var bytesRead: Int
                                        var copiedBytes = 0L
                                        var lastUpdate = System.currentTimeMillis()
                                        val totalBytes = entry.size
                                        
                                        fos.use { output ->
                                            while (tarIn.read(buffer).also { bytesRead = it } != -1) {
                                                output.write(buffer, 0, bytesRead)
                                                copiedBytes += bytesRead
                                                
                                                val now = System.currentTimeMillis()
                                                if (now - lastUpdate > 500) {
                                                    val progress = if (totalBytes > 0) ((copiedBytes * 100) / totalBytes).toInt() else -1
                                                    val copiedMb = copiedBytes / (1024f * 1024f)
                                                    val totMb = if (totalBytes > 0) totalBytes / (1024f * 1024f) else 0f
                                                    _downloadState.value = DownloadState.Downloading(progress, copiedMb, totMb)
                                                    lastUpdate = now
                                                }
                                            }
                                        }
                                        break
                                    }
                                    entry = tarIn.nextTarEntry
                                }
                                if (!foundBin) {
                                    _downloadState.value = DownloadState.Error("Keine .bin-Datei im .tar.gz-Archiv gefunden!")
                                    return@launch
                                }
                            }
                        }
                    }
                } else {
                    // Plain Direct File Copy
                    val destFile = File(outputDir, fileName.ifEmpty { "gemma-model.bin" })
                    val fos = FileOutputStream(destFile)
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    var copiedBytes = 0L
                    var lastUpdate = System.currentTimeMillis()
                    
                    inputStream.use { input ->
                        fos.use { output ->
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                copiedBytes += bytesRead
                                
                                val now = System.currentTimeMillis()
                                if (now - lastUpdate > 500) {
                                    val progress = if (totalBytesRaw > 0) ((copiedBytes * 100) / totalBytesRaw).toInt() else -1
                                    val copiedMb = copiedBytes / (1024f * 1024f)
                                    val totMb = if (totalBytesRaw > 0) totalBytesRaw / (1024f * 1024f) else 0f
                                    _downloadState.value = DownloadState.Downloading(progress, copiedMb, totMb)
                                    lastUpdate = now
                                }
                            }
                        }
                    }
                }
                
                _downloadState.value = DownloadState.Finished
                checkModelStatus()
                
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error("Kopieren fehlgeschlagen: ${e.message}")
            }
        }
    }

    fun fetchAndSummarizeNews() {
        viewModelScope.launch {
            val sources = db.feedSourceDao().getAllFeedSources()
            for (source in sources) {
                val fetchedItems = rssService.fetchFeed(source.url)
                val entities = fetchedItems.map {
                    ArticleEntity(
                        feedId = source.id,
                        title = it.title,
                        link = it.link,
                        content = it.description,
                        pubDate = it.pubDate
                    )
                }
                db.articleDao().insertArticles(entities)
            }
        }
    }

    private fun startInferenceWorker() {
        viewModelScope.launch {
            while (true) {
                // Wait until model is ready
                if (!_isModelReady.value) {
                    kotlinx.coroutines.delay(2000)
                    continue
                }

                // Fetch oldest/newest unsummarized item (prioritize newest visible UI item)
                val nextArticle = db.articleDao().getNextUnsummarizedArticle()
                if (nextArticle == null) {
                    // Queue empty, chill
                    _isSummarizing.value = false
                    kotlinx.coroutines.delay(2000)
                    continue
                }

                _isSummarizing.value = true
                
                try {
                    val chunks = aiService.chunkText(nextArticle.content)
                    val combinedSummary = StringBuilder()
                    
                    for (chunk in chunks) {
                        val partialSummary = aiService.summarize(chunk)
                        combinedSummary.append(partialSummary).append("\n")
                    }
                    
                    val finalSummary = combinedSummary.toString().trim()
                    // Never leave summary exact null, to avoid endless loops on failed generation
                    val safeSummary = finalSummary.ifEmpty { "Generierung fehlgeschlagen." }
                    
                    db.articleDao().updateArticle(nextArticle.copy(summary = safeSummary))
                } catch (e: Exception) {
                    db.articleDao().updateArticle(nextArticle.copy(summary = "Fehler bei der Generierung: ${e.message}"))
                }
                
                // Slight pause to let GPU cool down and UI thread to catch up frames
                kotlinx.coroutines.delay(500)
            }
        }
    }

    fun addFeedSource(name: String, url: String) {
        viewModelScope.launch {
            db.feedSourceDao().insertFeedSource(FeedSourceEntity(name = name, url = url))
        }
    }

    fun deleteFeedSource(source: FeedSourceEntity) {
        viewModelScope.launch {
            db.feedSourceDao().deleteFeedSource(source)
        }
    }

    fun getModelSizeString(): String {
        val modelDir = downloader.getModelDirectory()
        val binFiles = modelDir.listFiles { _, name -> name.endsWith(".bin") }
        var totalSize = 0L
        if (binFiles != null && binFiles.isNotEmpty()) {
            totalSize += binFiles[0].length()
        }
        
        if (totalSize > 0) {
            val sizeMb = totalSize / (1024f * 1024f)
            return "%.2f MB".format(sizeMb)
        }
        return "Not downloaded"
    }

    fun deleteModelCache() {
        val modelDir = downloader.getModelDirectory()
        modelDir.listFiles { _, name -> name.endsWith(".bin") }?.forEach { it.delete() }
        _isModelReady.value = false
        _downloadState.value = DownloadState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        aiService.close()
    }
}
