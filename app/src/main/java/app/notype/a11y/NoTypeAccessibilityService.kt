package app.notype.a11y

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import app.notype.logic.TextSplice
import app.notype.overlay.BubbleOverlay
import java.util.ArrayDeque

class NoTypeAccessibilityService : AccessibilityService() {
    private var cached: AccessibilityNodeInfo? = null
    private var hostedBubble: BubbleOverlay? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        val lostHost = hostedBubble != null
        hideHostedBubble()
        if (instance === this) instance = null
        cached?.recycle()
        cached = null
        if (lostHost) hostLost?.invoke()
        super.onDestroy()
    }

    fun showHostedBubble(
        x: Int,
        y: Int,
        onTap: () -> Unit,
        onCancel: () -> Unit,
        onMoved: (Int, Int) -> Unit,
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
        )
        return try {
            created.show(x, y)
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
            return
        }
        val root = rootInActiveWindow ?: return
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null && focused.isEditable && !focused.isPassword) {
            remember(focused)
        }
    }

    fun insertIntoFocusedField(text: String): Boolean {
        val node = usableNode() ?: return false
        if (node.isPassword) return false
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
        val clipboard = getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("noType", text))
        if (node.performAction(AccessibilityNodeInfo.ACTION_PASTE)) return true
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        return node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
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
        private var instance: NoTypeAccessibilityService? = null

        private var hostLost: (() -> Unit)? = null

        fun connected(): Boolean = instance != null

        fun insert(text: String): Boolean = instance?.insertIntoFocusedField(text) == true

        fun showHostedBubble(
            x: Int,
            y: Int,
            onTap: () -> Unit,
            onCancel: () -> Unit,
            onMoved: (Int, Int) -> Unit,
        ): BubbleOverlay? = instance?.showHostedBubble(x, y, onTap, onCancel, onMoved)

        fun hideHostedBubble() {
            instance?.hideHostedBubble()
        }

        fun setHostLostListener(listener: (() -> Unit)?) {
            hostLost = listener
        }
    }
}
