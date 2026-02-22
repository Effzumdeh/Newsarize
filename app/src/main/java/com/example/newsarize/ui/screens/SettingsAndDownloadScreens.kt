package com.example.newsarize.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.newsarize.ui.viewmodel.NewsViewModel

import com.example.newsarize.domain.downloader.DownloadState

@Composable
fun DownloadScreen(viewModel: NewsViewModel) {
    val downloadState by viewModel.downloadState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.importModelFromUri(it)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Sideload Model Required",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Zur Nutzung der On-Device KI musst du das Google MediaPipe Gemma 2B Modell von Kaggle/HuggingFace laden.\n\nDu kannst direkt das originale '.tar.gz' Archiv (z.B. gemma-2b-it-gpu-int4.tar.gz oder gemma-2b-it-gpu-int8.tar.gz) auswählen. Die App wird die '.bin' automatisch entpacken.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            when (val state = downloadState) {
                is DownloadState.Idle -> {
                    Button(onClick = { filePickerLauncher.launch("*/*") }) {
                        Text("Select Downloaded Model File")
                    }
                }
                is DownloadState.Downloading -> {
                    LinearProgressIndicator(
                        progress = state.progress / 100f,
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Copying from Downloads: ${state.progress}%% (%.1f / %.1f MB)".format(state.downloadedMb, state.totalMb),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                is DownloadState.Processing -> {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Download complete! Finalizing model (please wait)...\nThis can take a few seconds.",
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                is DownloadState.Finished -> {
                    Text(
                        "✅ Ready! Loading AI Engine...",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                is DownloadState.Error -> {
                    Text(
                        "❌ Error: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { filePickerLauncher.launch("*/*") }) {
                        Text("Select Model File Again")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Keep the app open during download.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NewsViewModel,
    onBack: () -> Unit
) {
    val feeds by viewModel.feedSources.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var newFeedName by remember { mutableStateOf("") }
    var newFeedUrl by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Text("+")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                "Model Management",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Gemma 2B (MediaPipe)", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Status: ${if (isModelReady) "Installed (MediaPipe Tasks Ready)" else "Not Installed"}", style = MaterialTheme.typography.bodyMedium)
                    Text("Size: ${viewModel.getModelSizeString()}", style = MaterialTheme.typography.bodyMedium)
                    
                    if (isModelReady) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.deleteModelCache() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete AI Model")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "RSS Feeds",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            feeds.forEach { feed ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(feed.name, style = MaterialTheme.typography.titleMedium)
                        Text(feed.url, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { viewModel.deleteFeedSource(feed) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
                Divider()
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Add Feed") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newFeedName,
                            onValueChange = { newFeedName = it },
                            label = { Text("Name (e.g. Heise)") }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newFeedUrl,
                            onValueChange = { newFeedUrl = it },
                            label = { Text("RSS URL") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (newFeedName.isNotBlank() && newFeedUrl.isNotBlank()) {
                            viewModel.addFeedSource(newFeedName, newFeedUrl)
                            showDialog = false
                            newFeedName = ""
                            newFeedUrl = ""
                        }
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
