package app.ossflow.ui

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.ossflow.a11y.AccessibilityEnabled
import app.ossflow.a11y.OSSFlowAccessibilityService

data class PermissionSnapshot(
    val microphone: Boolean,
    val notifications: Boolean,
    val overlay: Boolean,
    val accessibility: Boolean,
) {
    val readyForBubble: Boolean = microphone && notifications && (overlay || accessibility)
}

fun permissionSnapshot(context: Context): PermissionSnapshot {
    val mic = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    val notifications = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
    val overlay = Settings.canDrawOverlays(context)
    val component = ComponentName(context, OSSFlowAccessibilityService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    )
    val listed = AccessibilityEnabled.listed(enabled, component.packageName, component.className)
    val bound = context.getSystemService(AccessibilityManager::class.java)
        ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        ?.any { info ->
            val service = info.resolveInfo?.serviceInfo ?: return@any false
            val name = service.name ?: return@any false
            val className = if (name.startsWith('.')) service.packageName + name else name
            service.packageName == component.packageName && className == component.className
        } == true
    val accessibility = listed || bound || OSSFlowAccessibilityService.connected()
    return PermissionSnapshot(mic, notifications, overlay, accessibility)
}

@Composable
fun rememberPermissionSnapshot(): PermissionSnapshot {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var tick by remember { mutableIntStateOf(0) }
    val serviceConnected by OSSFlowAccessibilityService.connectedFlow.collectAsState()
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return remember(tick, serviceConnected) { permissionSnapshot(context) }
}

fun openOverlaySettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    )
    context.startActivity(intent)
}

fun openAccessibilitySettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
}

fun openAppInfo(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}"),
    )
    context.startActivity(intent)
}
