package com.flozaxx.applock.lock

import android.accessibilityservice.AccessibilityService
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import com.flozaxx.applock.R

/**
 * An opaque cover the accessibility service puts over a locked app the instant it appears.
 * Starting [LockActivity] takes a few hundred milliseconds; without the cover the app's content
 * would be readable for that long.
 */
class Curtain(private val service: AccessibilityService) {

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var view: View? = null

    fun show() {
        handler.removeCallbacksAndMessages(null)
        if (view == null) {
            val cover = View(service).apply {
                setBackgroundColor(service.getColor(R.color.lock_window_background))
                // Swallow touches so nothing in the app underneath can be pressed blind.
                isClickable = true
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.OPAQUE,
            )
            windowManager.addView(cover, params)
            view = cover
        }
        // Security/UX: the cover sits above every activity, including the lock screen. If that never
        // reports in, remove the cover anyway rather than leave the phone unusable.
        handler.postDelayed(::hide, MAX_VISIBLE_MILLIS)
    }

    fun hide() {
        handler.removeCallbacksAndMessages(null)
        view?.let(windowManager::removeView)
        view = null
    }

    private companion object {
        const val MAX_VISIBLE_MILLIS = 1500L
    }
}
