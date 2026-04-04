package com.gpcreativestudios.scriptq.ui

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import com.gpcreativestudios.scriptq.R
import kotlinx.coroutines.*

class FloatingPromptService : Service() {

    private var mWindowManager: WindowManager? = null
    private var mFloatingWidget: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    private var isPlaying = false
    private var scrollDelay = 50L
    private var scrollJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val scriptText = intent?.getStringExtra("SCRIPT_TEXT") ?: ""
        showFloatingWidget(scriptText)
        return START_NOT_STICKY
    }

    private fun showFloatingWidget(text: String) {
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val layoutInflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mFloatingWidget = layoutInflater.inflate(R.layout.layout_floating_widget, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        val scrollView = mFloatingWidget?.findViewById<ScrollView>(R.id.script_scroll_view)
        val textView = mFloatingWidget?.findViewById<TextView>(R.id.script_text)
        textView?.text = text

        val playPauseBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.play_pause_btn)
        val speedUpBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.speed_up_btn)
        val slowDownBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.slow_down_btn)
        val closeButton = mFloatingWidget?.findViewById<ImageButton>(R.id.close_btn)

        playPauseBtn?.setOnClickListener {
            isPlaying = !isPlaying
            if (isPlaying) {
                playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
                startScrolling(scrollView!!)
            } else {
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
                scrollJob?.cancel()
            }
        }

        speedUpBtn?.setOnClickListener {
            scrollDelay = (scrollDelay - 10L).coerceAtLeast(10L)
        }

        slowDownBtn?.setOnClickListener {
            scrollDelay = (scrollDelay + 10L).coerceAtMost(200L)
        }

        closeButton?.setOnClickListener {
            stopSelf()
        }

        mWindowManager?.addView(mFloatingWidget, params)
    }

    private fun startScrolling(scrollView: ScrollView) {
        scrollJob?.cancel()
        scrollJob = serviceScope.launch {
            while (isActive && isPlaying) {
                scrollView.smoothScrollBy(0, 2)
                delay(scrollDelay)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        if (mFloatingWidget != null) {
            mWindowManager?.removeView(mFloatingWidget)
        }
    }
}
