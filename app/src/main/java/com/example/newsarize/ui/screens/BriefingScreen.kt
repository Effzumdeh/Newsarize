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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.newsarize.data.local.entity.ArticleUiModel
import com.example.newsarize.ui.viewmodel.NewsViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.HorizontalDivider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BriefingScreen(
    viewModel: NewsViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToBrowser: (url: String, title: String) -> Unit
) {
    val articles by viewModel.articles.collectAsState()
    val feedSources by viewModel.feedSources.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedFeedId by viewModel.selectedFeedId.collectAsState()
    val filterState by viewModel.filterState.collectAsState()
    val selectedCategoryId by viewModel.selectedCategoryId.collectAsState()
    
    var showFeedMenu by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is NewsViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is NewsViewModel.UiEvent.ScrollToTop -> {
                    try {
                        listState.animateScrollToItem(0)
                    } catch (e: Exception) {}
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
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
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Button Row (Read/Unread)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                val options = listOf("Alle", "Ungelesen", "Gelesen")
                val filterStates = listOf("ALL", "UNREAD", "READ")
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                        onClick = { viewModel.setFilterState(filterStates[index]) },
                        selected = filterState == filterStates[index]
                    ) {
                        Text(label)
                    }
                }
            }

            // Category Chips with Checkmark
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { viewModel.setSelectedCategory(null) },
                        label = { Text("Alle Themen") },
                        leadingIcon = if (selectedCategoryId == null) {
                            { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null
                    )
                }
                lazyItems(categories) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.name,
                        onClick = { viewModel.setSelectedCategory(category.name) },
                        label = { Text(category.name) },
                        leadingIcon = if (selectedCategoryId == category.name) {
                            { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                        } else null
                    )
                }
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
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(articles, key = { it.id }) { article ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.toggleArticleReadStatus(article)
                                    false // Don't actually dismiss the item, just toggle status
                                } else {
                                    false
                                }
                            }
                        )

                        LaunchedEffect(article.isRead) {
                            dismissState.reset()
                        }

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = if (article.isRead) Color.Gray else MaterialTheme.colorScheme.primary
                                val icon = if (article.isRead) Icons.Default.MarkEmailUnread else Icons.Default.Mail
                                val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color.copy(alpha = 0.2f))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = alignment
                                ) {
                                    Icon(icon, contentDescription = null, tint = color)
                                }
                            }
                        ) {
                            ElevatedCard(
                                onClick = { 
                                    viewModel.setArticleRead(article.id)
                                    onNavigateToBrowser(article.link, article.title)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .alpha(if (article.isRead) 0.6f else 1.0f),
                                shape = ShapeDefaults.Large
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = article.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.Bold,
                                        color = if (article.isRead) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                                    )
                                    
                                    article.category?.let { cat ->
                                        Spacer(modifier = Modifier.height(6.dp))
                                        SuggestionChip(
                                            onClick = { },
                                            label = { Text(cat) },
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val sdf = SimpleDateFormat("dd.MM. yyyy - HH:mm", Locale.getDefault())
                                    Text(
                                        text = "${feedSources.find { it.id == article.feedId }?.name ?: "Unknown"} | ${sdf.format(Date(article.pubDate))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = ShapeDefaults.Medium,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "🤖 KI-Zusammenfassung",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
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
        }
    }
}
