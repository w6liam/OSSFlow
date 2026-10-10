package app.notype.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.notype.container
import app.notype.model.AppSettings
import app.notype.overlay.BubbleRuntime
import app.notype.overlay.BubbleService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BubbleControls(modifier: Modifier = Modifier, showTitle: Boolean = true) {
    val context = LocalContext.current
    val snapshot = rememberPermissionSnapshot()
    val settings by context.container.settings.flow.collectAsState(initial = null as AppSettings?)
    val wanted = settings?.bubbleWanted ?: true
    val snoozed = rememberBubbleSnoozed()
    val scope = rememberCoroutineScope()
    var showHelp by remember { mutableStateOf(false) }
    LaunchedEffect(
        settings?.bubbleWanted,
        snapshot.microphone,
        snapshot.notifications,
        snapshot.overlay,
        snapshot.accessibility,
    ) {
        val turnOn = settings?.bubbleWanted ?: return@LaunchedEffect
        if (turnOn && snapshot.readyForBubble && !BubbleRuntime.running.value) {
            BubbleService.show(context)
        }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && permissionSnapshot(context).readyForBubble) BubbleService.show(context)
    }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) return@rememberLauncherForActivityResult
        val now = permissionSnapshot(context)
        when {
            !now.notifications -> notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            now.overlay || now.accessibility -> BubbleService.show(context)
            else -> openAppInfo(context)
        }
    }

    Column(modifier) {
        if (!snapshot.readyForBubble || !snapshot.accessibility) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Setup", style = MaterialTheme.typography.titleMedium)
                    PermissionRow("Microphone", snapshot.microphone) {
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                    PermissionRow("Notifications", snapshot.notifications) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    PermissionRow("Display over other apps", snapshot.overlay) {
                        openOverlaySettings(context)
                    }
                    PermissionRow("Accessibility", snapshot.accessibility) {
                        openAccessibilitySettings(context)
                    }
                    TextButton(onClick = { showHelp = true }) { Text("Need help?") }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (showTitle) {
                    Text("Bubble", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    if (snoozed) "Bubble is snoozed" else "Shows when the keyboard is open",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = wanted,
                onCheckedChange = { want ->
                    scope.launch { context.container.settings.setBubbleWanted(want) }
                    if (!want) {
                        BubbleService.hide(context)
                        return@Switch
                    }
                    val current = permissionSnapshot(context)
                    when {
                        !current.microphone -> micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        !current.notifications -> notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        current.overlay || current.accessibility -> BubbleService.show(context)
                        else -> openAppInfo(context)
                    }
                },
            )
        }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Need help?") },
            text = {
                Text(
                    "1. Open app info. Tap the menu, then Allow restricted settings. Confirm with your screen lock.\n" +
                        "2. Allow Display over other apps and Accessibility.\n" +
                        "3. Accessibility types into the field. If overlay stays off, Accessibility can still show the bubble.\n" +
                        "4. Android 17 can block Accessibility for apps that are not a verified tool. History still keeps the text.",
                )
            },
            confirmButton = {
                TextButton(onClick = { openAppInfo(context) }) { Text("Open app info") }
            },
            dismissButton = {
                TextButton(onClick = { showHelp = false }) { Text("Close") }
            },
        )
    }
}

@Composable
internal fun rememberBubbleSnoozed(): Boolean {
    val context = LocalContext.current
    val settings by context.container.settings.flow.collectAsState(initial = AppSettings.defaults())
    val running by BubbleRuntime.running.collectAsState()
    val runtimeSnoozed by BubbleRuntime.snoozed.collectAsState()
    return if (running) runtimeSnoozed else settings.bubbleSnoozed
}

internal fun endBubbleSnooze(context: Context, scope: CoroutineScope) {
    BubbleRuntime.snoozed.value = false
    BubbleRuntime.onSnoozeChanged?.invoke()
    scope.launch {
        context.container.settings.setBubbleSnoozed(false)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onAllow: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (granted) {
            Text(
                "Allowed",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            TextButton(onClick = onAllow) { Text("Allow") }
        }
    }
}
