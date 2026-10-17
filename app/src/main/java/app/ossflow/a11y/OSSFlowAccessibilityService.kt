package app.ossflow.a11y

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import app.ossflow.logic.TextSplice
import app.ossflow.overlay.BubbleOverlay
import java.util.ArrayDeque
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class OSSFlowAccessibilityService : AccessibilityService() {
    private var cached: AccessibilityNodeInfo? = null
    private var hostedBubble: BubbleOverlay? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refreshPresence = Runnable { reportPresence() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        connectedState.value = true
        if (presenceListener != null) schedulePresence()
    }

    override fun onDestroy() {
        handler.removeCallbacks(refreshPresence)
        val lostHost = hostedBubble != null
        hideHostedBubble()
        if (instance === this) {
            instance = null
            connectedState.value = false
        }
        cached?.recycle()
        cached = null
        presenceListener?.invoke(false, false, false)
        if (lostHost) hostLost?.invoke()
        super.onDestroy()
    }

    fun showHostedBubble(
        x: Int,
        y: Int,
        onTap: () -> Unit,
        onCancel: () -> Unit,
        onMoved: (Int, Int) -> Unit,
        onSnooze: () -> Unit,
    ): BubbleOverlay? {
        val existing = hostedBubble
        if (existing != null) {
            return try {
                existing.show(x, y)
                existing
            } catch (_: RuntimeException) {
                hideHostedBubble()
                null
            }
        }
        val created = BubbleOverlay(
            context = this,
            windowManager = getSystemService(WindowManager::class.java),
            windowType = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            onTap = onTap,
            onCancel = onCancel,
            onMoved = onMoved,
            onSnooze = onSnooze,
        )
        return try {
            created.show(x, y, present = false)
            hostedBubble = created
            created
        } catch (_: RuntimeException) {
            created.hide()
            null
        }
    }

    fun hideHostedBubble() {
        hostedBubble?.hide()
        hostedBubble = null
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val source = event.source
        if (source != null && source.isEditable && !source.isPassword) {
            remember(source)
        } else {
            source?.recycle()
            val root = rootInActiveWindow
            if (root != null) {
                val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                val kept = focused != null && focused.isEditable && !focused.isPassword
                if (kept) {
                    remember(focused)
                } else if (focused != null && focused !== root) {
                    focused.recycle()
                }
                if (!kept && root !== cached) root.recycle()
            }
        }
        schedulePresence()
    }

    private fun schedulePresence() {
        handler.removeCallbacks(refreshPresence)
        handler.postDelayed(refreshPresence, PRESENCE_DELAY_MS)
    }

    private fun reportPresence() {
        val listener = presenceListener ?: return
        val (editable, password, keyboard) = readField()
        listener(editable, password, keyboard)
    }

    private fun readField(): Triple<Boolean, Boolean, Boolean> {
        var editable = false
        var password = false
        var keyboard = false
        val wins = windows ?: return Triple(false, false, false)
        for (window in wins) {
            if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) keyboard = true
            if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION) {
                val root = window.root
                if (root != null) {
                    val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
                    val kept = focused != null && focused.isEditable && !focused.isPassword
                    if (kept) {
                        editable = true
                        remember(focused)
                    } else {
                        if (focused != null && focused.isPassword) password = true
                        if (focused != null && focused !== root) focused.recycle()
                    }
                    if (!kept && root !== cached) root.recycle()
                }
            }
            window.recycle()
        }
        if (editable) password = false
        return Triple(editable, password, keyboard)
    }

    fun insertIntoFocusedField(text: String): Boolean {
        val node = usableNode() ?: return false
        if (node.isPassword) return false
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        node.refresh()
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("OSSFlow", text))
        if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) return true
        val exposed = node.text
        if (exposed != null) {
            val merged = TextSplice.splice(
                exposed.toString(),
                node.textSelectionStart,
                node.textSelectionEnd,
                text,
            )
            val args = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, merged)
            }
            if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) return true
        }
        return false
    }

    private fun usableNode(): AccessibilityNodeInfo? {
        val held = cached
        if (held != null && held.refresh() && held.isEditable && !held.isPassword) return held
        val root = rootInActiveWindow ?: return null
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable && !focused.isPassword) return focused
        return findFocusedEditable(root)
    }

    private fun findFocusedEditable(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(root to 0)
        while (queue.isNotEmpty()) {
            val (node, depth) = queue.removeFirst()
            if (node.isEditable && node.isFocused && !node.isPassword) return node
            if (depth >= 12) continue
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                queue.add(child to depth + 1)
            }
        }
        return null
    }

    private fun remember(node: AccessibilityNodeInfo) {
        if (cached !== node) cached?.recycle()
        cached = node
    }

    companion object {
        @Volatile
        private var instance: OSSFlowAccessibilityService? = null

        private val connectedState = MutableStateFlow(false)
        val connectedFlow: StateFlow<Boolean> = connectedState

        private var hostLost: (() -> Unit)? = null
        private var presenceListener: ((Boolean, Boolean, Boolean) -> Unit)? = null
        private const val PRESENCE_DELAY_MS = 200L

        fun connected(): Boolean = instance != null

        fun setPresenceListener(listener: ((Boolean, Boolean, Boolean) -> Unit)?) {
            presenceListener = listener
            val service = instance ?: return
            if (listener == null) service.handler.removeCallbacks(service.refreshPresence)
            else service.schedulePresence()
        }

        fun insert(text: String): Boolean = instance?.insertIntoFocusedField(text) == true

        fun showHostedBubble(
            x: Int,
            y: Int,
            onTap: () -> Unit,
            onCancel: () -> Unit,
            onMoved: (Int, Int) -> Unit,
            onSnooze: () -> Unit,
        ): BubbleOverlay? = instance?.showHostedBubble(x, y, onTap, onCancel, onMoved, onSnooze)

        fun hideHostedBubble() {
            instance?.hideHostedBubble()
        }

        fun setHostLostListener(listener: (() -> Unit)?) {
            hostLost = listener
        }
    }
}
