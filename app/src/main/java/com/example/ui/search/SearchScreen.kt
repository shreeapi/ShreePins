package com.example.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.PinItem
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.MasonrySkeletonGrid
import com.example.ui.components.PinCard
import com.example.ui.components.PinContextMenuSheet
import com.example.ui.components.SkeletonPinCard
import com.example.ui.settings.GridDensity
import com.example.utils.ShareHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val POPULAR_SEARCHES = listOf(
    "Radha Krishna",
    "Cyberpunk City",
    "Anime Aesthetic",
    "Minimalist Architecture",
    "Dark Wallpaper 4K",
    "Indie Room Decor",
    "Studio Ghibli",
    "Street Photography",
    "Vintage Porsche",
    "Coffee Aesthetic"
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    initialQuery: String?,
    gridDensity: GridDensity,
    onPinClick: (PinItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val savedPinIds by viewModel.savedPinIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedPinForMenu by remember { mutableStateOf<PinItem?>(null) }
    val gridState = rememberLazyStaggeredGridState()
    var pendingDownloadPin by remember { mutableStateOf<PinItem?>(null) }
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingDownloadPin?.let { viewModel.downloadPin(it) }
        } else {
            Toast.makeText(context, "Storage permission is needed to save to Gallery", Toast.LENGTH_SHORT).show()
        }
        pendingDownloadPin = null
    }

    fun startDownload(pin: PinItem) {
        val permissionNeeded = if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        } else null

        if (permissionNeeded != null &&
            androidx.core.content.ContextCompat.checkSelfPermission(context, permissionNeeded) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownloadPin = pin
            permissionLauncher.launch(permissionNeeded)
        } else {
            viewModel.downloadPin(pin)
        }
    }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank() && uiState.activeSearchTerm != initialQuery) {
            viewModel.executeSearch(initialQuery)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
        }
    }

    // Infinite scroll
    val shouldLoadMore by remember {
        derivedStateOf {
            val total = gridState.layoutInfo.totalItemsCount
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && last >= total - 6
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && !uiState.endOfResultsReached && !uiState.isLoading) {
            viewModel.loadNextPage()
        }
    }

    val columns = when (gridDensity) {
        GridDensity.COMPACT -> 3
        GridDensity.COMFORTABLE -> 2
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = {
                        Text(
                            text = "Search radha, wallpapers, aesthetic...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.clearQuery() }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            viewModel.executeSearch()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_input")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Case 1: Search not yet executed or query cleared -> Show History & Popular Suggestions
            if (!uiState.isSearchingStarted && uiState.pins.isEmpty() && !uiState.isLoading) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    // Recent Searches Section
                    if (searchHistory.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.clearAllHistory() },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Clear All", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        items(searchHistory) { historyItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        focusManager.clearFocus()
                                        viewModel.executeSearch(historyItem)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = historyItem,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteHistoryItem(historyItem) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(20.dp)) }
                    }

                    // Popular Suggestions Section
                    item {
                        Row(
                            modifier = Modifier.padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Popular on ShreePins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            POPULAR_SEARCHES.forEach { term ->
                                SuggestionChip(
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.executeSearch(term)
                                    },
                                    label = {
                                        Text(text = term, style = MaterialTheme.typography.bodyMedium)
                                    },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    border = null
                                )
                            }
                        }
                    }
                }
            } else if (uiState.isLoading) {
                // Loading Skeleton Grid
                MasonrySkeletonGrid(
                    columns = columns,
                    itemCount = 8,
                    contentPadding = PaddingValues(12.dp)
                )
            } else if (uiState.error != null && uiState.pins.isEmpty()) {
                ErrorStateView(
                    message = uiState.error ?: "Couldn't load images",
                    onRetry = { viewModel.executeSearch() },
                    onTryAnotherSearch = { viewModel.clearQuery() }
                )
            } else if (uiState.pins.isEmpty()) {
                // Empty search results
                EmptyStateView(
                    icon = Icons.Outlined.Search,
                    title = "No images found",
                    subtitle = "We couldn't find matches for \"${uiState.activeSearchTerm}\". Try searching another term like Radha, Anime, or Wallpapers.",
                    actionLabel = "Explore Popular",
                    onAction = { viewModel.executeSearch("Radha Krishna") }
                )
            } else {
                // Staggered Grid of Search Results
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(
                        items = uiState.pins,
                        key = { it.id }
                    ) { pin ->
                        val isSaved = savedPinIds.contains(pin.id)
                        PinCard(
                            pin = pin,
                            isSaved = isSaved,
                            onCardClick = { onPinClick(pin) },
                            onCardLongClick = { selectedPinForMenu = pin },
                            onToggleSave = { viewModel.toggleSave(pin) },
                            onMoreClick = { selectedPinForMenu = pin }
                        )
                    }

                    if (uiState.isLoadingMore) {
                        items(columns) { index ->
                            SkeletonPinCard(
                                height = if (index % 2 == 0) 200.dp else 240.dp
                            )
                        }
                    }

                    if (uiState.endOfResultsReached && uiState.pins.isNotEmpty()) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✨ You've reached the end of results",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedPinForMenu?.let { pin ->
        val isSaved = savedPinIds.contains(pin.id)
        PinContextMenuSheet(
            pin = pin,
            isSaved = isSaved,
            onDismiss = { selectedPinForMenu = null },
            onView = {
                selectedPinForMenu = null
                onPinClick(pin)
            },
            onToggleSave = { viewModel.toggleSave(pin) },
            onDownload = { startDownload(pin) },
            onShareImage = {
                scope.launch { ShareHelper.shareImage(context, pin) }
            },
            onSharePin = { ShareHelper.sharePinLink(context, pin) },
            onCopyLink = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Pin Link", pin.originalUrl)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Image link copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
