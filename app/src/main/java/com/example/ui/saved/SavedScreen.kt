package com.example.ui.saved

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.PinItem
import com.example.ui.components.EmptyStateView
import com.example.ui.components.PinCard
import com.example.ui.components.PinContextMenuSheet
import com.example.ui.settings.GridDensity
import com.example.utils.ShareHelper
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: SavedViewModel,
    gridDensity: GridDensity,
    onPinClick: (PinItem) -> Unit,
    onNavigateToExplore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedPins by viewModel.savedPins.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedPinForMenu by remember { mutableStateOf<PinItem?>(null) }
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

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Saved",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (savedPins.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ) {
                                Text(
                                    text = savedPins.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (savedPins.isNotEmpty() || uiState.isSearchActive) {
                        IconButton(
                            onClick = { viewModel.toggleSearch() },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                imageVector = if (uiState.isSearchActive) Icons.Outlined.Close else Icons.Outlined.Search,
                                contentDescription = "Search saved",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = uiState.isSearchActive,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        placeholder = {
                            Text("Filter saved collection...", style = MaterialTheme.typography.bodyMedium)
                        },
                        leadingIcon = {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (savedPins.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.BookmarkBorder,
                    title = if (uiState.searchQuery.isNotBlank()) "No matching saved pins" else "Nothing saved yet",
                    subtitle = if (uiState.searchQuery.isNotBlank()) "No items match \"${uiState.searchQuery}\"" else "Tap the heart icon on any pin to store your favorite aesthetic discoveries offline.",
                    actionLabel = if (uiState.searchQuery.isNotBlank()) "Clear Filter" else "Explore Feed",
                    onAction = {
                        if (uiState.searchQuery.isNotBlank()) {
                            viewModel.onSearchQueryChange("")
                        } else {
                            onNavigateToExplore()
                        }
                    }
                )
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 12.dp
                ) {
                    items(
                        items = savedPins,
                        key = { it.id }
                    ) { pin ->
                        PinCard(
                            pin = pin,
                            isSaved = true,
                            onCardClick = { onPinClick(pin) },
                            onCardLongClick = { selectedPinForMenu = pin },
                            onToggleSave = { viewModel.removeSavedPin(pin.id) },
                            onMoreClick = { selectedPinForMenu = pin }
                        )
                    }
                }
            }
        }
    }

    selectedPinForMenu?.let { pin ->
        PinContextMenuSheet(
            pin = pin,
            isSaved = true,
            onDismiss = { selectedPinForMenu = null },
            onView = {
                selectedPinForMenu = null
                onPinClick(pin)
            },
            onToggleSave = { viewModel.removeSavedPin(pin.id) },
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
