package app.ossflow.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import app.ossflow.MainActivity
import app.ossflow.OSSFlowApp
import app.ossflow.R
import app.ossflow.a11y.OSSFlowAccessibilityService
import app.ossflow.audio.Recorder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow

object BubbleRuntime {
    val running = MutableStateFlow(false)
    val phase = MutableStateFlow(BubblePhase.Idle)
    val snoozed = MutableStateFlow(false)
    var onSnoozeChanged: (() -> Unit)? = null
}

object BubbleNotifications {
    const val CHANNEL_ID = "bubble_status"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel("bubble")
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.bubble_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.bubble_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }
}

class BubbleService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    private var overlay: BubbleOverlay? = null
    private var hostedByAccessibility = false
    private var editableFocused = false
    private var passwordFocused = false
    private var keyboardVisible = false
    private lateinit var recorder: Recorder

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        BubbleNotifications.ensureChannel(this)
        recorder = (application as OSSFlowApp).container.recorder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notification("Tap the bubble to dictate")
        try {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
            )
        } catch (_: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (intent?.action == ACTION_HIDE) {
            scope.launch {
                (application as OSSFlowApp).container.settings.setBubbleSnoozed(false)
                BubbleRuntime.snoozed.value = false
                stopSelf()
            }
            return START_NOT_STICKY
        }
        showBubble()
        return START_STICKY
    }

    override fun onDestroy() {
        job?.cancel()
        if (::recorder.isInitialized && recorder.isRecording) recorder.cancel()
        if (::recorder.isInitialized) recorder.deleteLeftovers()
        OSSFlowAccessibilityService.setHostLostListener(null)
        OSSFlowAccessibilityService.setPresenceListener(null)
        overlay?.hide()
        if (hostedByAccessibility) OSSFlowAccessibilityService.hideHostedBubble()
        overlay = null
        hostedByAccessibility = false
        showing = false
        BubbleRuntime.onSnoozeChanged = null
        BubbleRuntime.running.value = false
        BubbleRuntime.phase.value = BubblePhase.Idle
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    private var showing = false

    private fun showBubble() {
        if (overlay != null || showing) return
        showing = true
        val mic = ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
        if (mic != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            showing = false
            toast("Allow the microphone.")
            stopSelf()
            return
        }
        val canOverlay = Settings.canDrawOverlays(this)
        if (!canOverlay && !OSSFlowAccessibilityService.connected()) {
            showing = false
            toast("Allow restricted settings, then turn the bubble on.")
            stopSelf()
            return
        }
        scope.launch {
            val settings = (application as OSSFlowApp).container.settings.flow.first()
            val shown = if (canOverlay) {
                showApplicationOverlay(settings.bubbleX, settings.bubbleY, settings.bubbleSnoozed)
            } else {
                showAccessibilityOverlay(settings.bubbleX, settings.bubbleY)
            }
            if (!shown) {
                showing = false
                toast("Could not show the bubble.")
                stopSelf()
                return@launch
            }
            BubbleRuntime.snoozed.value = settings.bubbleSnoozed
            BubbleRuntime.onSnoozeChanged = { applyPresence() }
            OSSFlowAccessibilityService.setPresenceListener(::onPresence)
            setPhase(BubblePhase.Idle, null)
            BubbleRuntime.running.value = true
        }
    }

    private fun snooze() {
        BubbleRuntime.snoozed.value = true
        scope.launch {
            val settings = (application as OSSFlowApp).container.settings
            settings.setBubbleSnoozed(true)
            if (!BubbleRuntime.snoozed.value) settings.setBubbleSnoozed(false)
        }
        applyPresence()
    }

    private fun onPresence(editable: Boolean, password: Boolean, keyboard: Boolean) {
        editableFocused = editable
        passwordFocused = password
        keyboardVisible = keyboard
        applyPresence()
    }

    private fun applyPresence() {
        overlay?.setPresent(
            BubblePresence.visible(
                editableFocused = editableFocused,
                password = passwordFocused,
                keyboardVisible = keyboardVisible,
                phase = BubbleRuntime.phase.value,
                accessibilityConnected = OSSFlowAccessibilityService.connected(),
                snoozed = BubbleRuntime.snoozed.value,
            ),
        )
    }

    private fun showApplicationOverlay(x: Int, y: Int, snoozed: Boolean): Boolean {
        hostedByAccessibility = false
        OSSFlowAccessibilityService.setHostLostListener(null)
        val window = overlay ?: BubbleOverlay(
            context = this,
            windowManager = getSystemService(WindowManager::class.java),
            windowType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            onTap = ::onBubbleTap,
            onCancel = ::cancelRecording,
            onMoved = ::rememberPosition,
            onSnooze = ::snooze,
        ).also { overlay = it }
        return try {
            window.show(
                x,
                y,
                present = !snoozed && !OSSFlowAccessibilityService.connected(),
            )
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun showAccessibilityOverlay(x: Int, y: Int): Boolean {
        OSSFlowAccessibilityService.setHostLostListener { stopSelf() }
        val window = OSSFlowAccessibilityService.showHostedBubble(
            x = x,
            y = y,
            onTap = ::onBubbleTap,
            onCancel = ::cancelRecording,
            onMoved = ::rememberPosition,
            onSnooze = ::snooze,
        ) ?: return false
        overlay = window
        hostedByAccessibility = true
        return true
    }

    private fun rememberPosition(x: Int, y: Int) {
        scope.launch {
            (application as OSSFlowApp).container.settings.setBubblePosition(x, y)
        }
    }

    private fun onBubbleTap() {
        when (BubbleRuntime.phase.value) {
            BubblePhase.Recording -> finishRecording()
            BubblePhase.Working -> Unit
            else -> startRecording()
        }
    }

    private fun startRecording() {
        if (BubbleRuntime.phase.value == BubblePhase.Working) return
        try {
            recorder.start()
            setPhase(BubblePhase.Recording, null)
        } catch (e: Exception) {
            setPhase(BubblePhase.Error, e.message ?: "Could not record.")
            scope.launch {
                delay(1400)
                if (BubbleRuntime.phase.value == BubblePhase.Error) setPhase(BubblePhase.Idle, null)
            }
        }
    }

    private fun finishRecording() {
        if (BubbleRuntime.phase.value != BubblePhase.Recording) return
        val recording = try {
            recorder.stop()
        } catch (e: Exception) {
            recorder.cancel()
            setPhase(BubblePhase.Error, e.message ?: "Could not stop recording.")
            scope.launch {
                delay(1400)
                setPhase(BubblePhase.Idle, null)
            }
            return
        }
        setPhase(BubblePhase.Working, null)
        val coordinator = (application as OSSFlowApp).container.coordinator
        job = scope.launch {
            try {
                val outcome = coordinator.process(recording.file, recording.durationMs)
                val message = if (outcome.inserted) {
                    if (outcome.cleanupSkipped) "Inserted. Cleanup failed." else "Inserted"
                } else {
                    "Copied. Paste it if it did not land."
                }
                setPhase(BubblePhase.Success, message)
                toast(message)
                delay(1200)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val message = e.message ?: "Dictation failed."
                setPhase(BubblePhase.Error, message)
                toast(message)
                delay(1600)
            } finally {
                recording.file.delete()
                if (BubbleRuntime.phase.value != BubblePhase.Idle) setPhase(BubblePhase.Idle, null)
            }
        }
    }

    private fun cancelRecording() {
        if (BubbleRuntime.phase.value != BubblePhase.Recording) return
        recorder.cancel()
        setPhase(BubblePhase.Idle, null)
        toast("Cancelled")
    }

    private fun setPhase(phase: BubblePhase, message: String?) {
        BubbleRuntime.phase.value = phase
        overlay?.setPhase(phase, message)
        applyPresence()
        val status = when (phase) {
            BubblePhase.Idle -> "Tap the bubble to dictate"
            BubblePhase.Recording -> "Listening"
            BubblePhase.Working -> "Transcribing"
            BubblePhase.Success -> message ?: "Done"
            BubblePhase.Error -> message ?: "Something went wrong"
        }
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(status))
    }

    private fun notification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val stop = PendingIntent.getService(
            this,
            1,
            Intent(this, BubbleService::class.java).setAction(ACTION_HIDE),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, BubbleNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mark_mono)
            .setContentTitle("OSSFlow")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(open)
            .addAction(0, "Stop", stop)
            .build()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION_SHOW = "app.ossflow.SHOW"
        const val ACTION_HIDE = "app.ossflow.HIDE"
        private const val NOTIFICATION_ID = 42

        fun show(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BubbleService::class.java).setAction(ACTION_SHOW),
            )
        }

        fun hide(context: Context) {
            val intent = Intent(context, BubbleService::class.java).setAction(ACTION_HIDE)
            if (BubbleRuntime.running.value) {
                context.startService(intent)
            } else {
                context.stopService(intent)
            }
        }
    }
}
