package com.reelcounter

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView

class ReelAccessibilityService : AccessibilityService() {

    companion object {
        const val IG = "com.instagram.android"
        const val PREFS = "reel_prefs"
        const val KEY_COUNT = "count"
        const val KEY_STRICT = "strict"
    }

    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences(PREFS, MODE_PRIVATE) }
    private var wm: WindowManager? = null
    private var overlay: TextView? = null
    private var lastCountTime = 0L
    private val settled = Runnable { onScrollSettled() }

    override fun onServiceConnected() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent) {
        val pkg = e.packageName?.toString() ?: return
        when (e.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (pkg == IG) showOverlay()
                else if (pkg != "com.android.systemui" && !pkg.contains("inputmethod")) hideOverlay()
            }
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (pkg == IG) {
                    showOverlay()
                    if (isReelsScreen()) {
                        handler.removeCallbacks(settled)
                        handler.postDelayed(settled, 500)
                    }
                }
            }
        }
    }

    // Scroll ruk gaya = ek reel badli, to count +1
    private fun onScrollSettled() {
        val now = SystemClock.elapsedRealtime()
        if (now - lastCountTime < 700) return
        lastCountTime = now
        prefs.edit().putInt(KEY_COUNT, prefs.getInt(KEY_COUNT, 0) + 1).apply()
        overlay?.text = label()
    }

    private fun isReelsScreen(): Boolean {
        if (!prefs.getBoolean(KEY_STRICT, true)) return true
        return hasClipsView(rootInActiveWindow, 0)
    }

    private fun hasClipsView(n: AccessibilityNodeInfo?, depth: Int): Boolean {
        if (n == null || depth > 12) return false
        val id = n.viewIdResourceName
        if (id != null && id.contains("clips_viewer")) return true
        for (i in 0 until n.childCount) {
            if (hasClipsView(n.getChild(i), depth + 1)) return true
        }
        return false
    }

    private fun label() = "🎬 " + prefs.getInt(KEY_COUNT, 0)

    private fun showOverlay() {
        if (overlay != null) return
        val tv = TextView(this).apply {
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(40, 16, 40, 16)
            background = GradientDrawable().apply {
                cornerRadius = 60f
                setColor(0xCC000000.toInt())
            }
            text = label()
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = 120
        }
        try { wm?.addView(tv, lp); overlay = tv } catch (_: Exception) {}
    }

    private fun hideOverlay() {
        overlay?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        overlay = null
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        hideOverlay()
        return super.onUnbind(intent)
    }
}
