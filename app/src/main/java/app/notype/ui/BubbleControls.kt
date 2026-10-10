package app.notype.ui

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
fun BubbleControls(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val snapshot = rememberPermissionSnapshot()
    val running by BubbleRuntime.running.collectAsState()
    val snoozed = rememberBubbleSnoozed()
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
                    Text(
                        "Android blocked Display over other apps because noType was installed outside the Play Store. Open noType’s app info, tap the ⋮ menu, and choose Allow restricted settings. Confirm with your screen lock. That menu appears after the denial you just saw. Then allow Display over other apps and Accessibility. Accessibility types into the field you are using. If Display over other apps still will not turn on, Accessibility alone can show the bubble, and dictated text is still copied here.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp, bottom = 4.dp),
                    )
                    TextButton(onClick = { openAppInfo(context) }) {
                        Text("Open app info")
                    }
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
                    Text(
                        "On Android 17, Advanced Protection blocks this for apps that are not a verified accessibility tool. Dictation still copies the text into History.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
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
                Text("Bubble", style = MaterialTheme.typography.titleMedium)
                Text(
                    if (snoozed) {
                        "Bubble is snoozed"
                    } else {
                        "Shows when a text field is selected and the keyboard is up. Tap to talk, tap to stop."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = running,
                onCheckedChange = { want ->
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
            text = if (granted) "$label allowed" else label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!granted) {
            TextButton(onClick = onAllow) { Text("Allow") }
        }
    }
}
