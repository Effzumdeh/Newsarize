package com.example.newsarize.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.newsarize.ui.viewmodel.NewsViewModel

import com.example.newsarize.domain.downloader.DownloadState

@Composable
fun DownloadScreen(viewModel: NewsViewModel) {
    val downloadState by viewModel.downloadState.collectAsState()
    val isModelReady by viewModel.isModelReady.collectAsState()
    val isModelInstalled by viewModel.isModelInstalled.collectAsState()

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
                if (isModelInstalled) "Sideload Model Found" else "Sideload Model Required",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Zur Nutzung der On-Device KI musst du das Google MediaPipe Gemma Modell (z.B. von Kaggle) laden.\n\nDu kannst direkt das originale '.tar.gz' Archiv oder eine entpackte .bin, .task, .tflite, oder .litertlm Datei auswählen. Die App wird Archive bei Bedarf automatisch entpacken.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            when (val state = downloadState) {
                is DownloadState.Idle -> {
                    Button(onClick = { filePickerLauncher.launch("*/*") }) {
                        Text(if (isModelInstalled) "Model ändern / aktualisieren" else "Select Downloaded Model File")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.initializeEngine() },
                        enabled = !isModelReady,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Manually Start AI Engine")
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
                "Das geladene Modell (.bin, .task, .tflite, .litertlm) wird sicher im App-Speicher " +
                "(${viewModel.getModelSizeString()}) isoliert aufbewahrt.",
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Feed")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
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
                        Text("Engine is running ✅", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = { viewModel.stopEngine() },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Text("Stop AI Engine")
                        }
                    } else {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Engine is stopped 🛑", style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.initializeEngine() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Start AI Engine")
                        }
                    }
                    
                    if (isModelReady) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { viewModel.deleteModelCache() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
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
                ListItem(
                    headlineContent = { Text(feed.name) },
                    supportingContent = { Text(feed.url) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteFeedSource(feed) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Themen (Tags)",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            val categories by viewModel.allCategories.collectAsState()
            var showTagDialog by remember { mutableStateOf(false) }
            var newTagName by remember { mutableStateOf("") }
            
            Spacer(modifier = Modifier.height(8.dp))
            categories.forEach { category ->
                ListItem(
                    headlineContent = { Text(category.name) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.deleteCategory(category) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            
            Button(
                onClick = { showTagDialog = true },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            ) {
                Text("Tag hinzufügen")
            }

            if (showTagDialog) {
                AlertDialog(
                    onDismissRequest = { showTagDialog = false },
                    title = { Text("Tag hinzufügen") },
                    text = {
                        OutlinedTextField(
                            value = newTagName,
                            onValueChange = { newTagName = it },
                            label = { Text("z.B. #Fussball") }
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            if (newTagName.isNotBlank()) {
                                viewModel.addCategory(newTagName)
                                showTagDialog = false
                                newTagName = ""
                            }
                        }) {
                            Text("Hinzufügen")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTagDialog = false }) {
                            Text("Abbrechen")
                        }
                    }
                )
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
