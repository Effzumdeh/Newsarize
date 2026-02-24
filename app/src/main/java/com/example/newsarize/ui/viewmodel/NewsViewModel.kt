package com.example.newsarize.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.newsarize.data.local.AppDatabase
import com.example.newsarize.data.local.entity.ArticleEntity
import com.example.newsarize.data.local.entity.ArticleUiModel
import com.example.newsarize.data.local.entity.FeedSourceEntity
import com.example.newsarize.data.network.RssService
import com.example.newsarize.domain.ai.MediaPipeAiService
import com.example.newsarize.domain.downloader.ModelDownloader
import com.example.newsarize.domain.downloader.DownloadState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private val _isModelInstalled = MutableStateFlow(false)
    val isModelInstalled = _isModelInstalled.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState = _downloadState.asStateFlow()

    private val _isSummarizing = MutableStateFlow(false)
    val isSummarizing = _isSummarizing.asStateFlow()

    private val _selectedFeedId = MutableStateFlow<Int?>(null)
    val selectedFeedId = _selectedFeedId.asStateFlow()

    private val _filterState = MutableStateFlow("ALL") // "ALL", "UNREAD", "READ"
    val filterState = _filterState.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val articles = combine(_selectedFeedId, _filterState, _selectedCategoryId) { feedId, filter, category ->
        Triple(feedId, filter, category)
    }.flatMapLatest { (feedId, filter, category) ->
        db.articleDao().getFilteredArticlesFlow(feedId, filter, category)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val feedSources = db.feedSourceDao().getAllFeedSourcesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val categories = combine(
        db.categoryDao().getAllCategoriesFlow(),
        db.articleDao().getUsedCategoriesFlow()
    ) { allCats, usedCats ->
        allCats.filter { usedCats.contains(it.name) }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allCategories = db.categoryDao().getAllCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _uiEvents = MutableSharedFlow<UiEvent>()
    val uiEvents = _uiEvents.asSharedFlow()

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()
        object ScrollToTop : UiEvent()
    }

    init {
        // We no longer call checkModelStatus() here to avoid warm-restart GPU crashes.
        // The user must manually start the engine or it starts via UI triggers.
        
        // Prefill default feeds and categories if empty
        viewModelScope.launch {
            if (db.feedSourceDao().getAllFeedSources().isEmpty()) {
                db.feedSourceDao().insertFeedSource(FeedSourceEntity(name = "Tagesschau", url = "https://www.tagesschau.de/xml/rss2"))
                db.feedSourceDao().insertFeedSource(FeedSourceEntity(name = "Heise", url = "https://www.heise.de/rss/heise-atom.xml"))
            }
            if (db.categoryDao().getCategoryCount() == 0) {
                listOf("#Politik", "#Tech", "#Wirtschaft", "#Lokal").forEach {
                    db.categoryDao().insertCategory(com.example.newsarize.data.local.entity.CategoryEntity(name = it))
                }
            }
        }
        
        // Start infinite inference worker
        startInferenceWorker()
    }

    private val validExtensions = listOf(".bin", ".task", ".tflite", ".litertlm")

    fun initializeEngine() {
        val modelDir = downloader.getModelDirectory()
        val binFiles = modelDir.listFiles { _, name -> validExtensions.any { name.endsWith(it) } }
        if (binFiles != null && binFiles.isNotEmpty()) {
            val fileModel = binFiles[0]
            
            viewModelScope.launch {
                _downloadState.value = DownloadState.Processing // Show "Loading Engine"
                try {
                    // WORKAROUND: Give GPU drivers extra time during manual start
                    kotlinx.coroutines.delay(800) 
                    
                    val success = aiService.initialize(fileModel)
                    if (success) {
                        _isModelReady.value = true
                        _downloadState.value = DownloadState.Finished
                    }
                } catch (e: Throwable) {
                    _isModelReady.value = false
                    _downloadState.value = DownloadState.Error("Initialisierung fehlgeschlagen. Tipp: App neustarten oder Modell erneut wählen. Details: ${e.message}")
                }
            }
        }
    }

    fun stopEngine() {
        aiService.close()
        _isModelReady.value = false
        _downloadState.value = DownloadState.Idle
    }

    fun checkModelStatus() {
        val modelDir = downloader.getModelDirectory()
        val binFiles = modelDir.listFiles { _, name -> validExtensions.any { name.endsWith(it) } }
        if (binFiles != null && binFiles.isNotEmpty()) {
            val fileModel = binFiles[0]
            if (fileModel.length() < 100 * 1024 * 1024) {
                fileModel.delete()
                _isModelReady.value = false
                _downloadState.value = DownloadState.Error("Modell war fehlerhaft (zu klein).")
            } else {
                // We found a model, but we DON'T auto-start. 
                // We just set the state to Idle so the UI shows "Ready to start".
                _isModelInstalled.value = true
                _isModelReady.value = false
                _downloadState.value = DownloadState.Idle
            }
        } else {
            _isModelInstalled.value = false
            _isModelReady.value = false
            _downloadState.value = DownloadState.Error("Kein Modell installiert.")
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

                // Clean old files
                outputDir.listFiles { _, name -> validExtensions.any { name.endsWith(it) } }?.forEach { it.delete() }
                
                val isTarGz = fileName.lowercase().endsWith(".tar.gz")
                
                if (isTarGz) {
                    inputStream.use { input ->
                        GZIPInputStream(input).use { gzipIn ->
                            TarArchiveInputStream(gzipIn).use { tarIn ->
                                var entry = tarIn.nextTarEntry
                                var foundBin = false
                                while (entry != null) {
                                    if (!entry.isDirectory && validExtensions.any { entry.name.endsWith(it) }) {
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
                                    _downloadState.value = DownloadState.Error("Keine valide Modelldatei (.bin, .task, .tflite) im .tar.gz-Archiv gefunden!")
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
                _isModelInstalled.value = true // Ensure UI recognizes file presence
                checkModelStatus()
                
            } catch (e: Exception) {
                _downloadState.value = DownloadState.Error("Kopieren fehlgeschlagen: ${e.message}")
            }
        }
    }

    fun fetchAndSummarizeNews() {
        viewModelScope.launch {
            val sources = db.feedSourceDao().getAllFeedSources()
            var totalNewArticles = 0
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
                val insertedIds = db.articleDao().insertArticles(entities)
                totalNewArticles += insertedIds.count { it != -1L }
            }
            if (totalNewArticles > 0) {
                _uiEvents.emit(UiEvent.ShowSnackbar("$totalNewArticles neue Artikel gefunden"))
                _uiEvents.emit(UiEvent.ScrollToTop)
            } else {
                _uiEvents.emit(UiEvent.ShowSnackbar("Keine neuen Artikel in abonnierten Feeds"))
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

                // Fetch oldest/newest unprocessed item
                val nextArticleId = db.articleDao().getNextUnprocessedArticleId()
                if (nextArticleId == null) {
                    _isSummarizing.value = false
                    kotlinx.coroutines.delay(2000)
                    continue
                }

                _isSummarizing.value = true
                
                try {
                    val nextArticle = db.articleDao().getArticleById(nextArticleId) ?: continue
                    
                    var currentSummary = nextArticle.summary
                    var currentCategory = nextArticle.category

                    // 1. Categorization (if missing)
                    if (currentCategory == null) {
                        val tags = db.categoryDao().getAllCategories().map { it.name }
                        if (tags.isNotEmpty()) {
                            currentCategory = aiService.categorize(nextArticle.title + "\n" + nextArticle.content, tags)
                        }
                    }

                    // 2. Summary (if missing)
                    if (currentSummary == null) {
                        val chunks = aiService.chunkText(nextArticle.content)
                        val combinedSummary = StringBuilder()
                        
                        for (chunk in chunks) {
                            val partialSummary = aiService.summarize(chunk)
                            combinedSummary.append(partialSummary).append("\n")
                        }
                        
                        val finalSummary = combinedSummary.toString().trim()
                        currentSummary = finalSummary.ifEmpty { "Generierung fehlgeschlagen." }
                    }
                    
                    // Update article with both summary and category. 
                    // Set content to empty to save space as per original code.
                    db.articleDao().updateArticle(nextArticle.copy(
                        summary = currentSummary, 
                        category = currentCategory,
                        content = ""
                    ))
                } catch (e: Exception) {
                    // Fallback: mark as "processed" with defaults if it fails multiple times? 
                    // For now, just wait and retry.
                }
                
                kotlinx.coroutines.delay(1500) // Yield CPU/GPU fully back to UI
            }
        }
    }

    fun setFilterState(state: String) { 
        _filterState.value = state 
        viewModelScope.launch { _uiEvents.emit(UiEvent.ScrollToTop) }
    }
    
    fun setSelectedFeed(feedId: Int?) { 
        _selectedFeedId.value = feedId 
        viewModelScope.launch { _uiEvents.emit(UiEvent.ScrollToTop) }
    }

    fun setSelectedCategory(category: String?) { 
        _selectedCategoryId.value = category 
        viewModelScope.launch { _uiEvents.emit(UiEvent.ScrollToTop) }
    }
    
    fun toggleArticleReadStatus(article: ArticleUiModel) {
        viewModelScope.launch {
            db.articleDao().updateArticleReadStatus(article.id, !article.isRead)
        }
    }

    fun setArticleRead(articleId: Int) {
        viewModelScope.launch {
            db.articleDao().updateArticleReadStatus(articleId, true)
        }
    }

    fun addCategory(name: String) {
        viewModelScope.launch {
            db.categoryDao().insertCategory(com.example.newsarize.data.local.entity.CategoryEntity(name = name))
        }
    }

    fun deleteCategory(category: com.example.newsarize.data.local.entity.CategoryEntity) {
        viewModelScope.launch {
            db.categoryDao().deleteCategory(category)
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
        val binFiles = modelDir.listFiles { _, name -> validExtensions.any { name.endsWith(it) } }
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
        aiService.close()
        val modelDir = downloader.getModelDirectory()
        modelDir.listFiles { _, name -> validExtensions.any { name.endsWith(it) } }?.forEach { it.delete() }
        _isModelReady.value = false
        _downloadState.value = DownloadState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        aiService.close()
    }
}
