package app.notype.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
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
) {
    private val density = context.resources.displayMetrics.density
    private val bubbleSize = dp(64)
    private val badgeSize = dp(22)
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private val ink = Color.parseColor("#07080D")
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
        background = disk(ink, signal)
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
        addView(circle, FrameLayout.LayoutParams(bubbleSize, bubbleSize))
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

    private val root = LinearLayout(context).apply {
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

    private val params = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        windowType,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false
    private var phase = BubblePhase.Idle

    fun show(x: Int, y: Int) {
        if (root.parent != null) return
        val bounds = windowManager.currentWindowMetrics.bounds
        params.x = if (x >= 0) x else bounds.width() - dp(80)
        params.y = if (y >= 0) y else (bounds.height() * 0.32f).toInt()
        clamp()
        windowManager.addView(root, params)
    }

    fun hide() {
        if (root.parent != null) windowManager.removeView(root)
    }

    fun setPhase(next: BubblePhase, message: String?) {
        phase = next
        when (next) {
            BubblePhase.Idle -> {
                circle.background = disk(ink, signal)
                icon.clearColorFilter()
                icon.setImageResource(R.drawable.ic_mark_n)
                icon.visibility = View.VISIBLE
                progress.visibility = View.GONE
                badge.visibility = View.INVISIBLE
                caption.visibility = View.GONE
                root.contentDescription = "noType, start dictation"
            }
            BubblePhase.Recording -> {
                circle.background = disk(Color.parseColor("#E11D48"))
                icon.setColorFilter(Color.WHITE)
                icon.setImageResource(R.drawable.ic_stop)
                icon.visibility = View.VISIBLE
                progress.visibility = View.GONE
                badge.visibility = View.VISIBLE
                showCaption(message)
                root.contentDescription = "Stop dictation"
            }
            BubblePhase.Working -> {
                circle.background = disk(ink, signal)
                icon.visibility = View.INVISIBLE
                progress.visibility = View.VISIBLE
                badge.visibility = View.INVISIBLE
                showCaption(message ?: "Transcribing")
                root.contentDescription = "Transcribing"
            }
            BubblePhase.Success -> {
                circle.background = disk(Color.parseColor("#15803D"))
                icon.setColorFilter(Color.WHITE)
                icon.setImageResource(R.drawable.ic_check)
                icon.visibility = View.VISIBLE
                progress.visibility = View.GONE
                badge.visibility = View.INVISIBLE
                showCaption(message)
                root.contentDescription = message ?: "Done"
            }
            BubblePhase.Error -> {
                circle.background = disk(Color.parseColor("#9F1239"))
                icon.setColorFilter(Color.WHITE)
                icon.setImageResource(R.drawable.ic_stop)
                icon.visibility = View.VISIBLE
                progress.visibility = View.GONE
                badge.visibility = View.INVISIBLE
                showCaption(message)
                root.contentDescription = message ?: "Dictation failed"
            }
        }
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
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                if (dragging && root.parent != null) {
                    params.x = startX + dx.roundToInt()
                    params.y = startY + dy.roundToInt()
                    clamp()
                    windowManager.updateViewLayout(root, params)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!dragging) {
                    if (phase == BubblePhase.Recording && inBadge(event)) onCancel() else onTap()
                } else {
                    onMoved(params.x, params.y)
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> return true
        }
        return false
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
