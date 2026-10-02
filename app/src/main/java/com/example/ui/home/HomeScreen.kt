package com.example.ui.home

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.domain.model.PinItem
import com.example.ui.components.ErrorStateView
import com.example.ui.components.MasonrySkeletonGrid
import com.example.ui.components.PinCard
import com.example.ui.components.PinContextMenuSheet
import com.example.ui.components.SkeletonPinCard
import com.example.ui.settings.GridDensity
import com.example.utils.ShareHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val CATEGORIES = listOf(
    "Trending",
    "Radha Krishna",
    "Wallpapers",
    "Aesthetic",
    "Architecture",
    "Nature",
    "Anime",
    "Minimalist",
    "Travel",
    "Quotes",
    "Art",
    "Cars",
    "Street Style"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    gridDensity: GridDensity,
    onNavigateToSearch: (String?) -> Unit,
    onNavigateToSettings: () -> Unit,
    onPinClick: (PinItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedPinIds by viewModel.savedPinIds.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedPinForMenu by remember { mutableStateOf<PinItem?>(null) }
    var pendingDownloadPin by remember { mutableStateOf<PinItem?>(null) }
    val gridState = rememberLazyStaggeredGridState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pendingDownloadPin?.let { viewModel.downloadPin(it) }
        } else {
            Toast.makeText(context, "Storage permission is needed to save to Gallery", Toast.LENGTH_SHORT).show()
        }
        pendingDownloadPin = null
    }

    fun startDownload(pin: PinItem) {
        val permissionNeeded = if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        } else null

        if (permissionNeeded != null &&
            ContextCompat.checkSelfPermission(context, permissionNeeded) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingDownloadPin = pin
            permissionLauncher.launch(permissionNeeded)
        } else {
            viewModel.downloadPin(pin)
        }
    }

    // Listen for snackbar events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
        }
    }

    // Infinite scroll listener: Trigger when nearing the bottom
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = gridState.layoutInfo.totalItemsCount
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleItem >= totalItems - 5
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingMore && !uiState.isLoadingInitial) {
            viewModel.loadNextPage()
        }
    }

    val columns = when (gridDensity) {
        GridDensity.COMPACT -> 3
        GridDensity.COMFORTABLE -> 2
    }

    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Logo and Title
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.shreepins_logo),
                            contentDescription = "ShreePins Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "ShreePins",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Search Button
                    IconButton(
                        onClick = { onNavigateToSearch(null) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Settings Button
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Horizontal Category Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORIES.forEach { category ->
                        val isSelected = uiState.selectedCategory.equals(category, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategory(category) },
                            label = {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = null
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoadingInitial -> {
                    MasonrySkeletonGrid(
                        columns = columns,
                        itemCount = 8,
                        contentPadding = PaddingValues(12.dp)
                    )
                }

                uiState.error != null && uiState.pins.isEmpty() -> {
                    ErrorStateView(
                        message = uiState.error ?: "Couldn't load images",
                        onRetry = { viewModel.retry() },
                        onTryAnotherSearch = { onNavigateToSearch(null) }
                    )
                }

                else -> {
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

                        // Bottom Pagination Skeleton Loader
                        if (uiState.isLoadingMore) {
                            items(columns) { index ->
                                SkeletonPinCard(
                                    height = if (index % 2 == 0) 200.dp else 240.dp
                                )
                            }
                        }

                        // Infinite scroll manual fallback trigger if needed
                        if (!uiState.isLoadingMore && uiState.pins.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.loadNextPage() },
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Text("Discover More Pins")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Long Press Context Menu
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
            onToggleSave = {
                viewModel.toggleSave(pin)
            },
            onDownload = {
                startDownload(pin)
            },
            onShareImage = {
                scope.launch {
                    ShareHelper.shareImage(context, pin)
                }
            },
            onSharePin = {
                ShareHelper.sharePinLink(context, pin)
            },
            onCopyLink = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Pin Link", pin.originalUrl)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Image link copied to clipboard", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
