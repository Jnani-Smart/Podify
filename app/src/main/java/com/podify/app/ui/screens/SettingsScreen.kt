package com.podify.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.podify.app.ui.components.*
import com.podify.app.ui.theme.*
import com.podify.app.util.PermissionHelper

/**
 * Settings screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(PodifySpacing.Default),
            verticalArrangement = Arrangement.spacedBy(PodifySpacing.Default)
        ) {
            // Permissions Section
            item {
                SectionHeader(title = "Permissions")
            }
            
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        title = "Bluetooth Settings",
                        subtitle = "Manage paired devices",
                        icon = Icons.Outlined.Bluetooth,
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                    
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = PodifySpacing.Default)
                    )
                    
                    SettingsRow(
                        title = "Overlay Permission",
                        subtitle = if (PermissionHelper.hasOverlayPermission(context)) 
                            "Granted" else "Tap to enable popup",
                        icon = Icons.Outlined.Layers,
                        onClick = {
                            PermissionHelper.openOverlaySettings(context)
                        },
                        trailing = {
                            if (PermissionHelper.hasOverlayPermission(context)) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = AppleGreen
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    )
                    
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = PodifySpacing.Default)
                    )
                    
                    SettingsRow(
                        title = "App Permissions",
                        subtitle = "Manage all permissions",
                        icon = Icons.Outlined.Security,
                        onClick = {
                            PermissionHelper.openAppSettings(context)
                        },
                        trailing = {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
            
            // About Section
            item {
                SectionHeader(title = "About")
            }
            
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    SettingsRow(
                        title = "Version",
                        subtitle = "1.0.0",
                        icon = Icons.Outlined.Info
                    )
                    
                    Divider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = PodifySpacing.Default)
                    )
                    
                    SettingsRow(
                        title = "Based on OpenPods",
                        subtitle = "Open source AirPods companion",
                        icon = Icons.Outlined.Code
                    )
                }
            }
            
            // Info Card
            item {
                Spacer(modifier = Modifier.height(PodifySpacing.Default))
                
                Surface(
                    shape = RoundedCornerShape(PodifyCorners.Medium),
                    color = AppleBlue.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(PodifySpacing.Default),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = AppleBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(PodifySpacing.Medium))
                        Text(
                            text = "Podify detects AirPods via Bluetooth Low Energy scanning. " +
                                   "Battery information is read from Apple's broadcast data. " +
                                   "Mode switching (ANC/Transparency) requires using the AirPods stem.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AppleBlue
                        )
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(PodifySpacing.XXLarge))
            }
        }
    }
}
