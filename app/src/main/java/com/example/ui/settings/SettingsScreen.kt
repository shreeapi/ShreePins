package com.example.ui.settings

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.repository.PinRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsManager: SettingsManager,
    pinRepository: PinRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themeMode by settingsManager.themeMode.collectAsStateWithLifecycle()
    val gridDensity by settingsManager.gridDensity.collectAsStateWithLifecycle()
    val downloadQuality by settingsManager.downloadQuality.collectAsStateWithLifecycle()

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Appearance Section
            SettingsSectionCard(title = "Appearance", icon = Icons.Outlined.Palette) {
                Column {
                    ThemeOptionRow(
                        title = "System Default",
                        selected = themeMode == ThemeMode.SYSTEM,
                        onSelect = { settingsManager.setThemeMode(ThemeMode.SYSTEM) }
                    )
                    ThemeOptionRow(
                        title = "Light Mode",
                        selected = themeMode == ThemeMode.LIGHT,
                        onSelect = { settingsManager.setThemeMode(ThemeMode.LIGHT) }
                    )
                    ThemeOptionRow(
                        title = "Dark Mode",
                        selected = themeMode == ThemeMode.DARK,
                        onSelect = { settingsManager.setThemeMode(ThemeMode.DARK) }
                    )
                }
            }

            // Grid Layout Section
            SettingsSectionCard(title = "Grid Layout", icon = Icons.Outlined.GridView) {
                Column {
                    ThemeOptionRow(
                        title = "Comfortable (2 Columns)",
                        selected = gridDensity == GridDensity.COMFORTABLE,
                        onSelect = { settingsManager.setGridDensity(GridDensity.COMFORTABLE) }
                    )
                    ThemeOptionRow(
                        title = "Compact (3 Columns)",
                        selected = gridDensity == GridDensity.COMPACT,
                        onSelect = { settingsManager.setGridDensity(GridDensity.COMPACT) }
                    )
                }
            }

            // Download Quality Section
            SettingsSectionCard(title = "Download Quality", icon = Icons.Outlined.Download) {
                Column {
                    ThemeOptionRow(
                        title = "Original Resolution (Highest)",
                        selected = downloadQuality == DownloadQuality.ORIGINAL,
                        onSelect = { settingsManager.setDownloadQuality(DownloadQuality.ORIGINAL) }
                    )
                    ThemeOptionRow(
                        title = "High Quality (736px)",
                        selected = downloadQuality == DownloadQuality.HIGH,
                        onSelect = { settingsManager.setDownloadQuality(DownloadQuality.HIGH) }
                    )
                    ThemeOptionRow(
                        title = "Medium Quality (474px)",
                        selected = downloadQuality == DownloadQuality.MEDIUM,
                        onSelect = { settingsManager.setDownloadQuality(DownloadQuality.MEDIUM) }
                    )
                }
            }

            // Storage & Data Section
            SettingsSectionCard(title = "Data & Storage", icon = Icons.Outlined.CleaningServices) {
                Column {
                    SettingsActionRow(
                        title = "Clear Image Cache",
                        subtitle = "Frees up temporary image storage",
                        icon = Icons.Outlined.CleaningServices,
                        onClick = {
                            val freed = settingsManager.clearImageCache()
                            val mb = freed / (1024 * 1024)
                            Toast.makeText(context, "Image cache cleaned ($mb MB freed)", Toast.LENGTH_SHORT).show()
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    SettingsActionRow(
                        title = "Clear Search History",
                        subtitle = "Remove all past search entries",
                        icon = Icons.Outlined.DeleteSweep,
                        onClick = { showClearHistoryDialog = true }
                    )
                }
            }

            // About Section
            SettingsSectionCard(title = "About", icon = Icons.Outlined.Info) {
                Column {
                    SettingsActionRow(
                        title = "About ShreePins",
                        subtitle = "Version 1.0.0 • Architecture & Credits",
                        icon = Icons.Outlined.Info,
                        onClick = { showAboutDialog = true }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Search History") },
            text = { Text("Are you sure you want to delete all saved search keywords?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch { pinRepository.clearSearchHistory() }
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Search history deleted", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.shreepins_logo),
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("ShreePins")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "A high-performance Pinterest-inspired image discovery app built with Kotlin, Jetpack Compose, Material 3, and Room persistence.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Image discovery API: AnshAPI",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Powered by AnshAPI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Designed for modern Android with Scoped Storage MediaStore gallery saving, FileProvider Sharesheet, and offline local caching.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onSelect)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
