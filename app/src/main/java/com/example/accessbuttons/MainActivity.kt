package com.example.accessbuttons

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.accessbuttons.ui.theme.AccessButtonsTheme

class MainActivity : ComponentActivity() {
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AccessButtonsTheme {
                PermissionManagerScreen()
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
    private fun PermissionManagerScreen() {
        var hasOverlayPermission by remember { mutableStateOf(false) }
        var batteryWhitelisted by remember { mutableStateOf(false) }
        var isXiaomiDevice by remember { mutableStateOf(false) }

        fun refreshState() {
            hasOverlayPermission = canDrawOverlays()
            batteryWhitelisted = isIgnoringBatteryOptimizations()
            isXiaomiDevice = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
                Build.MANUFACTURER.equals("Redmi", ignoreCase = true)
        }

        LaunchedEffect(Unit) {
            refreshState()
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = getString(R.string.permission_manager_title),
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = getString(R.string.permission_manager_subtitle),
                style = MaterialTheme.typography.bodyMedium
            )

            PermissionCard(
                title = getString(R.string.overlay_permission_title),
                description = getString(R.string.overlay_permission_description),
                enabled = hasOverlayPermission,
                buttonText = getString(R.string.open_settings),
                onClick = {
                    openOverlaySettings()
                    refreshState()
                }
            )

            if (isXiaomiDevice) {
                PermissionCard(
                    title = getString(R.string.autostart_permission_title),
                    description = getString(R.string.autostart_permission_description),
                    enabled = false,
                    buttonText = getString(R.string.open_autostart),
                    onClick = { openAutostartSettings() }
                )
            }

            PermissionCard(
                title = getString(R.string.battery_permission_title),
                description = getString(R.string.battery_permission_description),
                enabled = batteryWhitelisted,
                buttonText = getString(R.string.whitelist_app),
                onClick = {
                    openBatteryOptimizationSettings()
                    refreshState()
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    startOverlayService()
                    refreshState()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = hasOverlayPermission
            ) {
                Text(text = getString(R.string.start_floating_controller))
            }
            Button(
                onClick = ::stopOverlayService,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = getString(R.string.stop_floating_controller))
            }
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    enabled: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (enabled) {
                    stringResource(id = R.string.status_ready)
                } else {
                    stringResource(id = R.string.status_action_required)
                },
                style = MaterialTheme.typography.labelLarge
            )
            Button(onClick = onClick) {
                Text(text = buttonText)
            }
        }
    }
}
