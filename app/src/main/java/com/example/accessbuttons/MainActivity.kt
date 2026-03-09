package com.example.accessbuttons

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.accessbuttons.ui.theme.AccessButtonsTheme

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (VolumeService.isActive()) {
            sendResetStateToService()
        }

        enableEdgeToEdge()
        setContent {
            AccessButtonsTheme {
                PremiumPermissionManagerScreen()
            }
        }
    }

    private fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(this)

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val manager = getSystemService(PowerManager::class.java)
        return manager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun openOverlaySettings() {
        startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun openAutostartSettings() {
        val candidates = listOf(
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            },
            Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.appmanager.ApplicationsDetailsActivity"
                )
                putExtra("package_name", packageName)
            }
        )
        for (intent in candidates) {
            runCatching {
                startActivity(intent)
                return
            }
        }
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun sendResetStateToService() {
        val intent = Intent(this, VolumeService::class.java)
            .setAction(VolumeService.ACTION_RESET_STATE)
        startService(intent)
    }

    private fun startOverlayService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        val intent = Intent(this, VolumeService::class.java).setAction(VolumeService.ACTION_START)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopOverlayService() {
        val intent = Intent(this, VolumeService::class.java).setAction(VolumeService.ACTION_STOP)
        stopService(intent)
    }

    @Composable
    private fun PremiumPermissionManagerScreen() {
        val lifecycleOwner = LocalLifecycleOwner.current

        var hasOverlayPermission by remember { mutableStateOf(false) }
        var batteryWhitelisted by remember { mutableStateOf(false) }
        var isXiaomiDevice by remember { mutableStateOf(false) }

        fun refreshState() {
            hasOverlayPermission = canDrawOverlays()
            batteryWhitelisted = isIgnoringBatteryOptimizations()
            isXiaomiDevice = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                Build.MANUFACTURER.equals("Redmi", ignoreCase = true)
        }

        DisposableEffect(lifecycleOwner) {
            refreshState()
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) refreshState()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        val permissionItems = buildList {
            add(
                PermissionUiItem(
                    title = getString(R.string.overlay_permission_title),
                    description = getString(R.string.overlay_permission_description),
                    isReady = hasOverlayPermission,
                    buttonLabel = getString(R.string.open_settings),
                    onClick = ::openOverlaySettings
                )
            )
            if (isXiaomiDevice) {
                add(
                    PermissionUiItem(
                        title = getString(R.string.autostart_permission_title),
                        description = getString(R.string.autostart_permission_description),
                        isReady = false,
                        buttonLabel = getString(R.string.open_autostart),
                        onClick = ::openAutostartSettings
                    )
                )
            }
            add(
                PermissionUiItem(
                    title = getString(R.string.battery_permission_title),
                    description = getString(R.string.battery_permission_description),
                    isReady = batteryWhitelisted,
                    buttonLabel = getString(R.string.whitelist_app),
                    onClick = ::openBatteryOptimizationSettings
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0B1220),
                            Color(0xFF121A2A),
                            Color(0xFF1D2638)
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HeroCard(
                        title = getString(R.string.home_title),
                        subtitle = getString(R.string.home_subtitle)
                    )
                }
                item {
                    ControllerActionsCard(
                        canStart = hasOverlayPermission,
                        onLaunch = {
                            startOverlayService()
                            refreshState()
                        },
                        onShutdown = ::stopOverlayService
                    )
                }
                item {
                    Text(
                        text = getString(R.string.permission_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFE2E8F0),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(permissionItems) { item ->
                    PermissionCard(item)
                }
                item { Spacer(modifier = Modifier.height(10.dp)) }
            }
        }
    }
}

private data class PermissionUiItem(
    val title: String,
    val description: String,
    val isReady: Boolean,
    val buttonLabel: String,
    val onClick: () -> Unit
)

@Composable
private fun HeroCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x22FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFFF8FAFC),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = Color(0xFFC7D2FE),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ControllerActionsCard(
    canStart: Boolean,
    onLaunch: () -> Unit,
    onShutdown: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (canStart) {
                    "Overlay Permission Ready"
                } else {
                    "Overlay Permission Needed"
                },
                color = if (canStart) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onLaunch,
                    enabled = canStart,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        disabledContainerColor = Color(0xFF334155),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "Launch Control")
                }
                Button(
                    onClick = onShutdown,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color(0xFFE2E8F0)
                    )
                ) {
                    Text(text = "Stop Control")
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(item: PermissionUiItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x11FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.title,
                color = Color(0xFFF1F5F9),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.description,
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = if (item.isReady) {
                    "Status: Ready"
                } else {
                    "Status: Action Required"
                },
                color = if (item.isReady) Color(0xFF86EFAC) else Color(0xFFFDE68A),
                style = MaterialTheme.typography.labelLarge
            )
            Button(
                onClick = item.onClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE2E8F0),
                    contentColor = Color(0xFF0F172A)
                ),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = item.buttonLabel)
            }
        }
    }
}
