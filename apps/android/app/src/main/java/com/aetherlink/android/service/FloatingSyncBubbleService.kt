package com.aetherlink.android.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import com.aetherlink.android.feature.clipboard.ClipboardSyncActivity
import kotlin.math.abs

class FloatingSyncBubbleService : Service() {

    private var windowManager: WindowManager? = null
    private var bubbleView: View? = null

    companion object {
        private const val TAG = "FloatingBubble"
        var isRunning: Boolean = false

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            try {
                val intent = Intent(context, FloatingSyncBubbleService::class.java)
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting FloatingSyncBubbleService", e)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, FloatingSyncBubbleService::class.java)
                context.stopService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping FloatingSyncBubbleService", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val density = resources.displayMetrics.density
            val sizePx = (50 * density).toInt()

            val params = WindowManager.LayoutParams(
                sizePx,
                sizePx,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = resources.displayMetrics.widthPixels - sizePx - (16 * density).toInt()
                y = (resources.displayMetrics.heightPixels * 0.45).toInt()
            }

            val frame = FrameLayout(this)
            val bgDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E60F172A")) // Slate 900 90%
                setStroke((2 * density).toInt(), Color.parseColor("#06B6D4")) // Cyan border
            }
            frame.background = bgDrawable

            val text = TextView(this).apply {
                text = "📋"
                textSize = 22f
                gravity = Gravity.CENTER
            }
            frame.addView(text, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))

            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var isMoving = false

            frame.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isMoving = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isMoving = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            windowManager?.updateViewLayout(frame, params)
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isMoving) {
                            // Clicked: trigger clipboard sync
                            val syncIntent = Intent(this, ClipboardSyncActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            startActivity(syncIntent)
                        }
                        true
                    }
                    else -> false
                }
            }

            windowManager?.addView(frame, params)
            bubbleView = frame
            isRunning = true
            Log.d(TAG, "Floating sync bubble created")
        } catch (e: Exception) {
            Log.e(TAG, "Failed creating floating sync bubble", e)
            stopSelf()
        }
    }

    override fun onDestroy() {
        bubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) {
                Log.w(TAG, "Error removing bubble view: ${e.message}")
            }
        }
        bubbleView = null
        isRunning = false
        super.onDestroy()
        Log.d(TAG, "Floating sync bubble destroyed")
    }
}
