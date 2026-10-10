package app.notype.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import app.notype.R
import kotlin.math.abs
import kotlin.math.roundToInt

enum class BubblePhase {
    Idle,
    Recording,
    Working,
    Success,
    Error,
}

class BubbleOverlay(
    private val context: Context,
    private val windowManager: WindowManager,
    private val windowType: Int,
    private val onTap: () -> Unit,
    private val onCancel: () -> Unit,
    private val onMoved: (x: Int, y: Int) -> Unit,
    private val onSnooze: () -> Unit,
) {
    private val density = context.resources.displayMetrics.density
    private val bubbleSize = dp(64)
    private val badgeSize = dp(22)
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private val longPressTimeout = ViewConfiguration.getLongPressTimeout().toLong()
    private val handler = Handler(Looper.getMainLooper())

    private val ink = Color.parseColor("#07080D")
    private val inkIdle = Color.argb(224, 0x07, 0x08, 0x0D)
    private val signal = Color.parseColor("#2EE6C7")
    private val paper = Color.parseColor("#F4F1EA")

    private val caption = TextView(context).apply {
        setTextColor(paper)
        textSize = 13f
        maxWidth = dp(220)
        maxLines = 3
        setPadding(dp(10), dp(6), dp(10), dp(6))
        background = roundRect(ink, dp(12))
        visibility = View.GONE
    }

    private val circle = View(context).apply {
        background = disk(inkIdle, signal)
    }

    private var ringPulse: ObjectAnimator? = null

    private val pulseRing = View(context).apply {
        background = disk(Color.TRANSPARENT, signal)
        visibility = View.GONE
    }

    private val icon = ImageView(context).apply {
        setImageResource(R.drawable.ic_mark_n)
        scaleType = ImageView.ScaleType.FIT_CENTER
    }

    private val progress = ProgressBar(context).apply {
        isIndeterminate = true
        indeterminateTintList = android.content.res.ColorStateList.valueOf(signal)
        visibility = View.GONE
    }

    private val badge = TextView(context).apply {
        text = "×"
        gravity = Gravity.CENTER
        setTextColor(Color.parseColor("#1C1B1F"))
        textSize = 14f
        includeFontPadding = false
        background = disk(Color.WHITE)
        visibility = View.INVISIBLE
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val bubble = FrameLayout(context).apply {
        clipChildren = false
        addView(circle, FrameLayout.LayoutParams(bubbleSize, bubbleSize))
        addView(pulseRing, FrameLayout.LayoutParams(bubbleSize, bubbleSize))
        addView(icon, FrameLayout.LayoutParams(dp(40), dp(40), Gravity.CENTER))
        addView(
            progress,
            FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER),
        )
        addView(
            badge,
            FrameLayout.LayoutParams(badgeSize, badgeSize, Gravity.TOP or Gravity.END),
        )
    }

    private val cluster = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        contentDescription = "noType dictate"
        addView(caption)
        addView(
            bubble,
            LinearLayout.LayoutParams(bubbleSize, bubbleSize).apply { topMargin = dp(6) },
        )
        setOnTouchListener { _, event -> handleTouch(event) }
    }

    private val snoozeTarget = TextView(context).apply {
        text = "Snooze"
        gravity = Gravity.CENTER
        setTextColor(paper)
        textSize = 16f
        setPadding(dp(28), dp(14), dp(28), dp(14))
        background = snoozeBackground(filled = false)
        visibility = View.GONE
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    private val root = FrameLayout(context).apply {
        addView(
            snoozeTarget,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
            ).apply { bottomMargin = dp(48) },
        )
        addView(
            cluster,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.START,
            ),
        )
    }

    private val baseFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        windowType,
        baseFlags,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false
    private var aiming = false
    private var fingerDown = false
    private var phase = BubblePhase.Idle
    private var present = true
    private val beginAim = Runnable {
        if (fingerDown && !dragging && phase == BubblePhase.Idle) startAiming()
    }

    fun show(x: Int, y: Int, present: Boolean = true) {
        if (root.parent != null) return
        val bounds = windowManager.currentWindowMetrics.bounds
        params.x = if (x >= 0) x else bounds.width() - dp(80)
        params.y = if (y >= 0) y else (bounds.height() * 0.32f).toInt()
        clamp()
        setPresent(present, added = false)
        windowManager.addView(root, params)
    }

    fun setPresent(show: Boolean) {
        setPresent(show, added = true)
    }

    private fun setPresent(show: Boolean, added: Boolean) {
        if (aiming && !show) return
        if (added && present == show) return
        present = show
        root.visibility = if (show) View.VISIBLE else View.GONE
        params.flags = if (show) baseFlags else baseFlags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        if (added && root.parent != null) {
            try {
                windowManager.updateViewLayout(root, params)
            } catch (_: RuntimeException) {
            }
        }
    }

    fun hide() {
        handler.removeCallbacks(beginAim)
        stopPulse()
        if (aiming) stopAiming(restore = true, updateWindow = false)
        if (root.parent != null) windowManager.removeView(root)
    }

    fun setPhase(next: BubblePhase, message: String?) {
        phase = next
        if (next == BubblePhase.Recording) startPulse() else stopPulse()
        when (next) {
            BubblePhase.Idle -> {
                circle.background = disk(inkIdle, signal)
                icon.clearColorFilter()
                icon.setImageResource(R.drawable.ic_mark_n)
                icon.visibility = View.VISIBLE
                progress.visibility = View.GONE
                badge.visibility = View.INVISIBLE
                caption.visibility = View.GONE
                cluster.contentDescription = "noType, start dictation"
            }
            BubblePhase.Recording -> {
                circle.background = disk(Color.parseColor("#E11D48"), signal)
                icon.setColorFilter(Color.WHITE)
                icon.setImageResource(R.drawable.ic_stop)
                icon.visibility = View.VISIBLE
                progress.visibility = View.GONE
                badge.visibility = View.VISIBLE
                caption.visibility = View.GONE
                cluster.contentDescription = "Stop dictation"
            }
            BubblePhase.Working -> {
                circle.background = disk(ink, signal)
                icon.visibility = View.INVISIBLE
                progress.visibility = View.VISIBLE
                badge.visibility = View.INVISIBLE
                caption.visibility = View.GONE
                cluster.contentDescription = "Transcribing"
            }
            BubblePhase.Success -> {
                circle.background = disk(Color.parseColor("#15803D"))
                icon.setColorFilter(Color.WHITE)
                icon.setImageResource(R.drawable.ic_check)
                icon.visibility = View.VISIBLE
                progress.visibility = View.GONE
                badge.visibility = View.INVISIBLE
                showCaption(message)
                cluster.contentDescription = message ?: "Done"
            }
            BubblePhase.Error -> {
                circle.background = disk(Color.parseColor("#9F1239"))
                icon.setColorFilter(Color.WHITE)
                icon.setImageResource(R.drawable.ic_stop)
                icon.visibility = View.VISIBLE
                progress.visibility = View.GONE
                badge.visibility = View.INVISIBLE
                showCaption(message)
                cluster.contentDescription = message ?: "Dictation failed"
            }
        }
    }

    private fun startPulse() {
        if (ringPulse?.isRunning == true) return
        pulseRing.visibility = View.VISIBLE
        ringPulse = ObjectAnimator.ofPropertyValuesHolder(
            pulseRing,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.72f, 1.08f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.72f, 1.08f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.95f, 0f),
        ).apply {
            duration = 1100
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    private fun stopPulse() {
        ringPulse?.cancel()
        ringPulse = null
        pulseRing.visibility = View.GONE
        pulseRing.alpha = 1f
        pulseRing.scaleX = 1f
        pulseRing.scaleY = 1f
    }

    private fun showCaption(message: String?) {
        if (message.isNullOrBlank()) {
            caption.visibility = View.GONE
        } else {
            caption.text = message
            caption.visibility = View.VISIBLE
        }
    }

    private fun handleTouch(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                startX = params.x
                startY = params.y
                dragging = false
                fingerDown = true
                if (phase == BubblePhase.Idle) handler.postDelayed(beginAim, longPressTimeout)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (aiming) {
                    moveAimed(dx, dy)
                } else {
                    if (!dragging && (abs(dx) > slop || abs(dy) > slop)) {
                        dragging = true
                        handler.removeCallbacks(beginAim)
                    }
                    if (dragging && root.parent != null) {
                        params.x = startX + dx.roundToInt()
                        params.y = startY + dy.roundToInt()
                        clamp()
                        windowManager.updateViewLayout(root, params)
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                handler.removeCallbacks(beginAim)
                fingerDown = false
                when {
                    aiming -> {
                        val snooze = overSnooze()
                        stopAiming(restore = true, updateWindow = true)
                        if (snooze) onSnooze()
                    }
                    !dragging -> {
                        if (phase == BubblePhase.Recording && inBadge(event)) onCancel() else onTap()
                    }
                    else -> onMoved(params.x, params.y)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(beginAim)
                fingerDown = false
                if (aiming) stopAiming(restore = true, updateWindow = true)
                return true
            }
        }
        return false
    }

    private fun startAiming() {
        if (root.parent == null || aiming) return
        aiming = true
        snoozeTarget.visibility = View.VISIBLE
        paintSnooze(false)
        params.width = WindowManager.LayoutParams.MATCH_PARENT
        params.height = WindowManager.LayoutParams.MATCH_PARENT
        params.x = 0
        params.y = 0
        placeCluster(startX, startY)
        updateWindow()
    }

    private fun moveAimed(dx: Float, dy: Float) {
        val bounds = windowManager.currentWindowMetrics.bounds
        val x = (startX + dx.roundToInt()).coerceIn(0, (bounds.width() - bubbleSize).coerceAtLeast(0))
        val y = (startY + dy.roundToInt()).coerceIn(
            0,
            (bounds.height() - bubbleSize - dp(6)).coerceAtLeast(0),
        )
        placeCluster(x, y)
        paintSnooze(overSnooze())
    }

    private fun stopAiming(restore: Boolean, updateWindow: Boolean) {
        aiming = false
        snoozeTarget.visibility = View.GONE
        paintSnooze(false)
        placeCluster(0, 0)
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        if (restore) {
            params.x = startX
            params.y = startY
        }
        clamp()
        if (updateWindow) updateWindow()
    }

    private fun placeCluster(x: Int, y: Int) {
        val lp = cluster.layoutParams as FrameLayout.LayoutParams
        lp.leftMargin = x
        lp.topMargin = y
        cluster.layoutParams = lp
    }

    private fun overSnooze(): Boolean {
        val lp = cluster.layoutParams as FrameLayout.LayoutParams
        val top = lp.topMargin + dp(6)
        val bubbleRect = Rect(lp.leftMargin, top, lp.leftMargin + bubbleSize, top + bubbleSize)
        val bounds = windowManager.currentWindowMetrics.bounds
        val pillWidth = snoozeTarget.width.coerceAtLeast(dp(140))
        val pillHeight = snoozeTarget.height.coerceAtLeast(dp(48))
        val pillLeft = (bounds.width() - pillWidth) / 2
        val pillTop = bounds.height() - dp(48) - pillHeight
        return Rect.intersects(
            bubbleRect,
            Rect(pillLeft, pillTop, pillLeft + pillWidth, pillTop + pillHeight),
        )
    }

    private fun paintSnooze(filled: Boolean) {
        snoozeTarget.background = snoozeBackground(filled)
        snoozeTarget.setTextColor(if (filled) ink else paper)
    }

    private fun snoozeBackground(filled: Boolean): GradientDrawable = GradientDrawable().apply {
        cornerRadius = dp(28).toFloat()
        if (filled) {
            setColor(signal)
        } else {
            setColor(Color.parseColor("#141821"))
            setStroke(dp(1), Color.parseColor("#2A3142"))
        }
    }

    private fun updateWindow() {
        if (root.parent == null) return
        try {
            windowManager.updateViewLayout(root, params)
        } catch (_: RuntimeException) {
        }
    }

    private fun inBadge(event: MotionEvent): Boolean {
        val top = bubble.top
        return event.x >= bubble.right - badgeSize &&
            event.y >= top &&
            event.y <= top + badgeSize
    }

    private fun clamp() {
        val bounds = windowManager.currentWindowMetrics.bounds
        params.x = params.x.coerceIn(0, (bounds.width() - bubbleSize).coerceAtLeast(0))
        params.y = params.y.coerceIn(0, (bounds.height() - bubbleSize).coerceAtLeast(0))
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()

    private fun disk(fill: Int, ring: Int? = null): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(fill)
        if (ring != null) setStroke(dp(3), ring)
    }

    private fun roundRect(color: Int, radius: Int): GradientDrawable = GradientDrawable().apply {
        cornerRadius = radius.toFloat()
        setColor(color)
    }
}
