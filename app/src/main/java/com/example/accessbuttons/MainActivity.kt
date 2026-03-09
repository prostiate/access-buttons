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
import androidx.compose.ui.res.stringResource
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
                HomeScreen()
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
    private fun HomeScreen() {
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

        val setupItems = buildList {
            add(
                SetupItem(
                    title = getString(R.string.overlay_permission_title),
                    description = getString(R.string.overlay_permission_description),
                    isReady = hasOverlayPermission,
                    buttonLabel = getString(R.string.open_settings),
                    onClick = ::openOverlaySettings
                )
            )
            if (isXiaomiDevice) {
                add(
                    SetupItem(
                        title = getString(R.string.autostart_permission_title),
                        description = getString(R.string.autostart_permission_description),
                        isReady = false,
                        buttonLabel = getString(R.string.open_autostart),
                        onClick = ::openAutostartSettings
                    )
                )
            }
            add(
                SetupItem(
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
                        listOf(
                            Color(0xFF090F1F),
                            Color(0xFF0E172B),
                            Color(0xFF111D33)
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HeaderCard(
                        title = getString(R.string.home_title),
                        subtitle = getString(R.string.home_subtitle)
                    )
                }
                item {
                    ControllerCard(
                        canActivate = hasOverlayPermission,
                        onActivate = {
                            startOverlayService()
                            refreshState()
                        },
                        onDeactivate = ::stopOverlayService
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
                items(setupItems.size) { index ->
                    SetupRow(item = setupItems[index])
                }
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

private data class SetupItem(
    val title: String,
    val description: String,
    val isReady: Boolean,
    val buttonLabel: String,
    val onClick: () -> Unit
)

@Composable
private fun HeaderCard(title: String, subtitle: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x1BFFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF8FAFC)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFCBD5E1)
            )
        }
    }
}

@Composable
private fun ControllerCard(
    canActivate: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x16FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (canActivate) {
                    "Controller Ready"
                } else {
                    "Overlay Permission Required"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (canActivate) Color(0xFF86EFAC) else Color(0xFFFCA5A5),
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onActivate,
                    enabled = canActivate,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        disabledContainerColor = Color(0xFF334155)
                    )
                ) {
                    Text(text = stringResource(id = R.string.action_activate_controller))
                }
                Button(
                    onClick = onDeactivate,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B1324),
                        contentColor = Color(0xFFE2E8F0)
                    )
                ) {
                    Text(text = stringResource(id = R.string.action_deactivate_controller))
                }
            }
        }
    }
}

@Composable
private fun SetupRow(item: SetupItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0x14FFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFF1F5F9),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )
                Text(
                    text = if (item.isReady) {
                        stringResource(id = R.string.status_ready)
                    } else {
                        stringResource(id = R.string.status_action_required)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (item.isReady) Color(0xFF86EFAC) else Color(0xFFFDE68A)
                )
            }
            Button(
                onClick = item.onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE2E8F0),
                    contentColor = Color(0xFF0F172A)
                )
            ) {
                Text(item.buttonLabel)
            }
        }
    }
}
