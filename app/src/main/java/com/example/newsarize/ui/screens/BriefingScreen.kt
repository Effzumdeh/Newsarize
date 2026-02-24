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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ListItem
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

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
    
    var showSourceSheet by remember { mutableStateOf(false) }
    var showTagSheet by remember { mutableStateOf(false) }
    
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
                    Text(
                        text = titleName,
                        modifier = Modifier.clickable { showSourceSheet = true }
                    ) 
                },
                actions = {
                    IconButton(onClick = { showTagSheet = true }) {
                        Icon(Icons.Default.List, contentDescription = "Filter Tags")
                    }
                    IconButton(onClick = { viewModel.fetchAndSummarizeNews() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh News")
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
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
                        bottom = padding.calculateBottomPadding() + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
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
                            var expanded by remember { mutableStateOf(false) }
                            
                            ElevatedCard(
                                onClick = { 
                                    viewModel.setArticleRead(article.id)
                                    onNavigateToBrowser(article.link, article.title)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .alpha(if (article.isRead) 0.6f else 1.0f)
                                    .animateContentSize(),
                                shape = ShapeDefaults.Large,
                                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.weight(1f)) {
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
                                        }

                                        if (!article.imageUrl.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.width(16.dp))
                                            AsyncImage(
                                                model = article.imageUrl,
                                                contentDescription = "Article Image",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(ShapeDefaults.Medium)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    val sdf = SimpleDateFormat("dd.MM. yyyy - HH:mm", Locale.getDefault())
                                    Text(
                                        text = "${feedSources.find { it.id == article.feedId }?.name ?: "Unknown"} | ${sdf.format(Date(article.pubDate))}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(
                                        onClick = { expanded = !expanded },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(if (expanded) "Hide AI Summary" else "Show AI Summary")
                                    }

                                    if (expanded) {
                                        Spacer(modifier = Modifier.height(8.dp))
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
        
        if (showSourceSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showSourceSheet = false },
                sheetState = sheetState
            ) {
                LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                    item {
                        Text(
                            "Select Feed Source",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider()
                    }
                    item {
                        ListItem(
                            headlineContent = { Text("Alle Quellen") },
                            modifier = Modifier.clickable {
                                viewModel.setSelectedFeed(null)
                                showSourceSheet = false
                            }
                        )
                    }
                    items(feedSources) { feed ->
                        ListItem(
                            headlineContent = { Text(feed.name) },
                            modifier = Modifier.clickable {
                                viewModel.setSelectedFeed(feed.id)
                                showSourceSheet = false
                            },
                            trailingContent = {
                                if (selectedFeedId == feed.id) {
                                    Icon(Icons.Default.Done, contentDescription = "Selected")
                                }
                            }
                        )
                    }
                }
            }
        }

        if (showTagSheet) {
            val sheetState = rememberModalBottomSheetState()
            ModalBottomSheet(
                onDismissRequest = { showTagSheet = false },
                sheetState = sheetState
            ) {
                LazyColumn(modifier = Modifier.padding(bottom = 24.dp)) {
                    item {
                        Text(
                            "Filter by Tag",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        HorizontalDivider()
                    }
                    item {
                        ListItem(
                            headlineContent = { Text("Alle Themen") },
                            modifier = Modifier.clickable {
                                viewModel.setSelectedCategory(null)
                                showTagSheet = false
                            },
                            trailingContent = {
                                if (selectedCategoryId == null) {
                                    Icon(Icons.Default.Done, contentDescription = "Selected")
                                }
                            }
                        )
                    }
                    items(categories) { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            modifier = Modifier.clickable {
                                viewModel.setSelectedCategory(category.name)
                                showTagSheet = false
                            },
                            trailingContent = {
                                if (selectedCategoryId == category.name) {
                                    Icon(Icons.Default.Done, contentDescription = "Selected")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
