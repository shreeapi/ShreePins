package com.example.navigation

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ShreePinsApplication
import com.example.domain.model.PinItem
import com.example.ui.components.NetworkBanner
import com.example.ui.detail.DetailScreen
import com.example.ui.detail.DetailViewModel
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.downloads.DownloadsViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.saved.SavedScreen
import com.example.ui.saved.SavedViewModel
import com.example.ui.search.SearchScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.settings.SettingsScreen

enum class NavDestination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    SEARCH("Search", Icons.Filled.Search, Icons.Outlined.Search),
    SAVED("Saved", Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder),
    DOWNLOADS("Downloads", Icons.Filled.Download, Icons.Outlined.Download),
    SETTINGS("Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun AppNavigation(
    app: ShreePinsApplication,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(NavDestination.HOME) }
    var selectedPinForDetail by remember { mutableStateOf<PinItem?>(null) }
    var searchInitialQuery by remember { mutableStateOf<String?>(null) }

    val homeViewModel = remember { HomeViewModel(app.pinRepository, app.imageDownloader) }
    val searchViewModel = remember { SearchViewModel(app.pinRepository, app.imageDownloader) }
    val savedViewModel = remember { SavedViewModel(app.pinRepository, app.imageDownloader) }
    val downloadsViewModel = remember { DownloadsViewModel(app.pinRepository) }
    val detailViewModel = remember { DetailViewModel(app.pinRepository, app.imageDownloader) }

    val isOnline by app.networkMonitor.isOnline.collectAsStateWithLifecycle(initialValue = true)
    val gridDensity by app.settingsManager.gridDensity.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Offline Notification Banner
            NetworkBanner(isOnline = isOnline)

            Box(modifier = Modifier.weight(1f)) {
                // Tab Content
                when (currentTab) {
                    NavDestination.HOME -> {
                        HomeScreen(
                            viewModel = homeViewModel,
                            gridDensity = gridDensity,
                            onNavigateToSearch = { query ->
                                searchInitialQuery = query
                                currentTab = NavDestination.SEARCH
                            },
                            onNavigateToSettings = {
                                currentTab = NavDestination.SETTINGS
                            },
                            onPinClick = { pin ->
                                selectedPinForDetail = pin
                            }
                        )
                    }

                    NavDestination.SEARCH -> {
                        SearchScreen(
                            viewModel = searchViewModel,
                            initialQuery = searchInitialQuery,
                            gridDensity = gridDensity,
                            onPinClick = { pin ->
                                selectedPinForDetail = pin
                            }
                        )
                    }

                    NavDestination.SAVED -> {
                        SavedScreen(
                            viewModel = savedViewModel,
                            gridDensity = gridDensity,
                            onPinClick = { pin ->
                                selectedPinForDetail = pin
                            },
                            onNavigateToExplore = {
                                currentTab = NavDestination.HOME
                            }
                        )
                    }

                    NavDestination.DOWNLOADS -> {
                        DownloadsScreen(
                            viewModel = downloadsViewModel,
                            gridDensity = gridDensity,
                            onPinClick = { pin ->
                                selectedPinForDetail = pin
                            },
                            onNavigateToHome = {
                                currentTab = NavDestination.HOME
                            }
                        )
                    }

                    NavDestination.SETTINGS -> {
                        SettingsScreen(
                            settingsManager = app.settingsManager,
                            pinRepository = app.pinRepository
                        )
                    }
                }
            }
        }

        // Floating Glass Transparent Bottom Navigation Bar
        if (selectedPinForDetail == null) {
            GlassBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Full-Screen Image Detail Overlay
        AnimatedVisibility(
            visible = selectedPinForDetail != null,
            enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 3 }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            selectedPinForDetail?.let { pin ->
                DetailScreen(
                    pin = pin,
                    viewModel = detailViewModel,
                    onNavigateBack = {
                        selectedPinForDetail = null
                    }
                )
            }
        }
    }
}

@Composable
fun GlassBottomNavBar(
    currentTab: NavDestination,
    onTabSelected: (NavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val glassBg = if (isDark) Color(0xCC181924) else Color(0xDDFFFFFF)
    val glassBorder = if (isDark) Color.White.copy(alpha = 0.16f) else Color.Black.copy(alpha = 0.08f)
    val view = LocalView.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        shape = CircleShape,
        color = glassBg,
        border = BorderStroke(1.2.dp, glassBorder),
        shadowElevation = 12.dp,
        tonalElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavDestination.entries.forEach { destination ->
                val isSelected = currentTab == destination
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1.0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "tab_scale"
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                            onTabSelected(destination)
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                        .testTag("nav_item_${destination.name.lowercase()}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 42.dp else 36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                            contentDescription = destination.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer(scaleX = scale, scaleY = scale)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = destination.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
