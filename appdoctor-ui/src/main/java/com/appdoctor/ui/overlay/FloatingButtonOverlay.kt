package com.appdoctor.ui.overlay

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import com.appdoctor.core.overlay.AppDoctorOverlay
import com.appdoctor.ui.R
import com.appdoctor.ui.dashboard.DashboardActivity
import java.util.WeakHashMap
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Draggable floating trigger implemented as a plain [View] added to **each Activity's own
 * [WindowManager]** using [WindowManager.LayoutParams.TYPE_APPLICATION].
 *
 * Why a plain View (not Compose) for the button:
 *  - No permission required (it lives inside the app's own window, not a system overlay).
 *  - No `ViewTree*Owner` plumbing, so it works over **any** Activity type, not just
 *    `ComponentActivity`.
 *  - Negligible CPU/memory footprint while idle.
 *
 * Touch behaviour: the window is sized [FrameLayout.LayoutParams.WRAP_CONTENT] and uses
 * `FLAG_NOT_TOUCH_MODAL` + `FLAG_NOT_FOCUSABLE`, so touches outside the button pass
 * straight through to the app; only touches on the button itself are consumed (for drag
 * or tap).
 *
 * All methods must be called on the main thread (guaranteed by the core coordinator).
 */
internal class FloatingButtonOverlay(
    context: Context,
) : AppDoctorOverlay {

    private val appContext: Context = context.applicationContext

    /** Live buttons keyed weakly by Activity so a leaked map entry can't leak the Activity. */
    private val buttons = WeakHashMap<Activity, View>()

    /** Last drag position, shared across activities so the button doesn't jump around. */
    private var lastX = UNSET
    private var lastY = UNSET

    override fun attach(activity: Activity) {
        if (buttons.containsKey(activity)) return

        val button = createButtonView(activity)
        val params = createLayoutParams(activity)
        button.setOnTouchListener(DragTouchListener(activity, params))

        runCatching { activity.windowManager.addView(button, params) }
            .onSuccess { buttons[activity] = button }
    }

    override fun detach(activity: Activity) {
        val button = buttons.remove(activity) ?: return
        runCatching { activity.windowManager.removeViewImmediate(button) }
    }

    override fun release() {
        // Copy keys first: removeView mutates the map.
        buttons.keys.toList().forEach(::detach)
        buttons.clear()
    }

    // ---- View construction ---------------------------------------------------------

    private fun createButtonView(context: Context): View {
        val sizePx = context.dp(BUTTON_SIZE_DP)
        val iconPadding = context.dp(BUTTON_ICON_PADDING_DP)

        val icon = ImageView(context).apply {
            setImageResource(R.drawable.appdoctor_ic_pulse)
            setPadding(iconPadding, iconPadding, iconPadding, iconPadding)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        return FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
            background = circleBackground()
            elevation = context.dp(BUTTON_ELEVATION_DP).toFloat()
            addView(
                icon,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                ),
            )
            contentDescription = "Open AppDoctor"
        }
    }

    private fun circleBackground(): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(BRAND_COLOR)
        setStroke(2, RING_COLOR)
    }

    private fun createLayoutParams(activity: Activity): WindowManager.LayoutParams {
        val sizePx = activity.dp(BUTTON_SIZE_DP)
        val metrics = activity.resources.displayMetrics
        if (lastX == UNSET || lastY == UNSET) {
            lastX = metrics.widthPixels - sizePx - activity.dp(DEFAULT_MARGIN_DP)
            lastY = (metrics.heightPixels * DEFAULT_VERTICAL_BIAS).roundToInt()
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = lastX
            y = lastY
        }
    }

    // ---- Drag + tap handling -------------------------------------------------------

    private inner class DragTouchListener(
        private val activity: Activity,
        private val params: WindowManager.LayoutParams,
    ) : View.OnTouchListener {

        private val touchSlop = ViewConfiguration.get(activity).scaledTouchSlop
        private var startParamsX = 0
        private var startParamsY = 0
        private var startRawX = 0f
        private var startRawY = 0f
        private var dragging = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startParamsX = params.x
                    startParamsY = params.y
                    startRawX = event.rawX
                    startRawY = event.rawY
                    dragging = false
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - startRawX
                    val dy = event.rawY - startRawY
                    if (!dragging && hypot(dx, dy) > touchSlop) dragging = true
                    if (dragging) {
                        params.x = clampX(activity, view, (startParamsX + dx).roundToInt())
                        params.y = clampY(activity, view, (startParamsY + dy).roundToInt())
                        runCatching { activity.windowManager.updateViewLayout(view, params) }
                    }
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        lastX = params.x
                        lastY = params.y
                    } else {
                        view.performClick()
                        openDashboard(activity)
                    }
                    return true
                }
            }
            return false
        }
    }

    private fun clampX(context: Context, view: View, value: Int): Int {
        val max = context.resources.displayMetrics.widthPixels - view.widthOrEstimate(context)
        return value.coerceIn(0, max.coerceAtLeast(0))
    }

    private fun clampY(context: Context, view: View, value: Int): Int {
        val max = context.resources.displayMetrics.heightPixels - view.heightOrEstimate(context)
        return value.coerceIn(0, max.coerceAtLeast(0))
    }

    private fun View.widthOrEstimate(context: Context): Int =
        if (width > 0) width else context.dp(BUTTON_SIZE_DP)

    private fun View.heightOrEstimate(context: Context): Int =
        if (height > 0) height else context.dp(BUTTON_SIZE_DP)

    private fun openDashboard(activity: Activity) {
        val intent = Intent(activity, DashboardActivity::class.java)
        runCatching { activity.startActivity(intent) }
    }

    private fun Context.dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        resources.displayMetrics,
    ).roundToInt()

    private companion object {
        private const val UNSET = Int.MIN_VALUE
        private const val BUTTON_SIZE_DP = 52
        private const val BUTTON_ICON_PADDING_DP = 12
        private const val BUTTON_ELEVATION_DP = 6
        private const val DEFAULT_MARGIN_DP = 12
        private const val DEFAULT_VERTICAL_BIAS = 0.35f

        private val BRAND_COLOR = "#4F9CF9".toColorInt()
        private val RING_COLOR = "#8FC1FF".toColorInt()
    }
}
