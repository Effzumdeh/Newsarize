package com.example.newsarize.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.example.newsarize.ui.viewmodel.NewsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefingScreen(
    viewModel: NewsViewModel,
    onNavigateToSettings: () -> Unit
) {
    val articles by viewModel.articles.collectAsState()
    val feedSources by viewModel.feedSources.collectAsState()
    val selectedFeedId by viewModel.selectedFeedId.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    
    var showFeedMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val titleName = feedSources.find { it.id == selectedFeedId }?.name ?: "Daily Briefing"
                    Text(titleName) 
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFeedMenu = true }) {
                            Icon(Icons.Default.List, contentDescription = "Filter Feed")
                        }
                        DropdownMenu(
                            expanded = showFeedMenu,
                            onDismissRequest = { showFeedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Alle Quellen") },
                                onClick = { 
                                    viewModel.setSelectedFeed(null)
                                    showFeedMenu = false
                                }
                            )
                            feedSources.forEach { feed ->
                                DropdownMenuItem(
                                    text = { Text(feed.name) },
                                    onClick = { 
                                        viewModel.setSelectedFeed(feed.id)
                                        showFeedMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.fetchAndSummarizeNews() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh News")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterState == "ALL",
                    onClick = { viewModel.setFilterState("ALL") },
                    label = { Text("Alle") }
                )
                FilterChip(
                    selected = filterState == "UNREAD",
                    onClick = { viewModel.setFilterState("UNREAD") },
                    label = { Text("Ungelesen") }
                )
                FilterChip(
                    selected = filterState == "READ",
                    onClick = { viewModel.setFilterState("READ") },
                    label = { Text("Gelesen") }
                )
            }

            if (articles.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Keine Nachrichten verfügbar.\nTippe auf Refresh.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(articles, key = { it.id }) { article ->
                        ElevatedCard(
                            onClick = { viewModel.toggleArticleReadStatus(article) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (article.isRead) 0.5f else 1.0f)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = article.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val sdf = SimpleDateFormat("dd.MM. yyyy - HH:mm", Locale.getDefault())
                                Text(
                                    text = "Quelle ID: ${article.feedId} | ${sdf.format(Date(article.pubDate))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = "🤖 KI-Zusammenfassung:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                val isSummarizingApp by viewModel.isSummarizing.collectAsState()
                                
                                if (article.summary == null && isSummarizingApp) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Generiere...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    Text(
                                        text = article.summary ?: "Ausstehend...",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
