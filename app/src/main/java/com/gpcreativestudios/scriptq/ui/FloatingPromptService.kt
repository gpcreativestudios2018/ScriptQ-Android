package com.gpcreativestudios.scriptq.ui

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.Html
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.gpcreativestudios.scriptq.R
import kotlinx.coroutines.*

import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.content.res.ColorStateList
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.Socket
import androidx.core.view.ViewCompat

class FloatingPromptService : Service() {

    private var mWindowManager: WindowManager? = null
    private var mFloatingWidget: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    private var isPlaying = false
    private var scrollDelay = 50L
    private var scrollJob: Job? = null
    
    // New state variables for customization
    private var textSizeSp = 18f
    private var opacityLevel = 0 // 0 = 100%, 1 = 75%, 2 = 50%
    private var isMirrored = false
    private var isInverted = false
    private var isPremium = false

    private var isRecording = false
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String = ""
    private var recordStartTime: Long = 0L

    // Voice Pacing properties
    private var isVoicePacing = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var speechIntent: Intent? = null
    private var voicePaceJob: Job? = null
    private var currentRmsdB: Float = -2f // Floor default

    // Remote Control properties
    private var isRemoteEnabled = false
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var serverSocket: ServerSocket? = null
    private var remoteJob: Job? = null

    companion object {
        private const val CHANNEL_ID = "FloatingPromptServiceChannel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "com.gpcreativestudios.scriptq.action.STOP_PROMPTER"
    }

    private fun setButtonTint(button: ImageButton?, color: Int) {
        button?.imageTintList = ColorStateList.valueOf(color)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        
        // Speed is stored as 1-20 in preferences. Need to map to delay (10L to 200L).
        // Pref: 5 -> Delay: 160L (calculated differently, let's keep it simple: 1=200L (slow), 20=10L (fast))
        // Re-using our formula from updateSpeedText: speedDisplay = (210 - scrollDelay) / 10
        // So: scrollDelay = 210 - (speedDisplay * 10)
        val defaultSpeedPref = prefs.getInt("default_scroll_speed", 5)
        scrollDelay = (210L - (defaultSpeedPref * 10L)).coerceIn(10L, 200L)
        
        textSizeSp = prefs.getInt("default_text_size", 18).toFloat()
        
        // Opacity is stored as string "0", "1", or "2" from ListPreference
        opacityLevel = prefs.getString("default_opacity", "0")?.toIntOrNull() ?: 0
        isPremium = PremiumAccess.isPremiumCached(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Floating Prompter Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val scriptText = intent?.getStringExtra("SCRIPT_TEXT") ?: ""
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ScriptQ Prompter")
            .setContentText("Prompter is active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        
        if (mFloatingWidget == null) {
            showFloatingWidget(scriptText)
        } else {
            // Update text if already showing
            val textView = mFloatingWidget?.findViewById<TextView>(R.id.script_text)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                textView?.text = Html.fromHtml(scriptText, Html.FROM_HTML_MODE_COMPACT)
            } else {
                @Suppress("DEPRECATION")
                textView?.text = Html.fromHtml(scriptText)
            }
            val scrollView = mFloatingWidget?.findViewById<ScrollView>(R.id.script_scroll_view)
            scrollView?.scrollTo(0, 0)
        }
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
        val widgetContainer = mFloatingWidget?.findViewById<View>(R.id.widget_container)
        val scriptSurface = mFloatingWidget?.findViewById<View>(R.id.script_surface)
        val dragHandle = mFloatingWidget?.findViewById<View>(R.id.drag_handle)
        val speedText = mFloatingWidget?.findViewById<TextView>(R.id.speed_text)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView?.text = Html.fromHtml(text, Html.FROM_HTML_MODE_COMPACT)
        } else {
            @Suppress("DEPRECATION")
            textView?.text = Html.fromHtml(text)
        }
        textView?.textSize = textSizeSp

        fun applyMirror() {
            textView?.scaleX = if (isMirrored) -1f else 1f
        }
        applyMirror()

        fun applyColors() {
            val textColor = if (isInverted) Color.BLACK else ContextCompat.getColor(this, R.color.text_primary)
            val surfaceColor = when (opacityLevel) {
                0 -> if (isInverted) Color.parseColor("#F2FFFFFF") else Color.parseColor("#CC0C1422")
                1 -> if (isInverted) Color.parseColor("#CCFFFFFF") else Color.parseColor("#990C1422")
                else -> if (isInverted) Color.parseColor("#99FFFFFF") else Color.parseColor("#730C1422")
            }

            textView?.setTextColor(textColor)
            speedText?.setTextColor(textColor)
            (scriptSurface ?: widgetContainer)?.let {
                ViewCompat.setBackgroundTintList(it, ColorStateList.valueOf(surfaceColor))
            }
        }
        applyColors()

        // 1. Drag functionality
        dragHandle?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        mWindowManager?.updateViewLayout(mFloatingWidget, params)
                        return true
                    }
                }
                return false
            }
        })

        val playPauseBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.play_pause_btn)
        val speedUpBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.speed_up_btn)
        val slowDownBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.slow_down_btn)
        val closeButton = mFloatingWidget?.findViewById<ImageButton>(R.id.close_btn)
        val advancedToggle = mFloatingWidget?.findViewById<TextView>(R.id.advanced_toggle)
        val advancedControlsContainer = mFloatingWidget?.findViewById<View>(R.id.advanced_controls_container)
        val resetBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.reset_btn)
        val increaseTextBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.text_size_increase_btn)
        val decreaseTextBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.text_size_decrease_btn)
        val opacityBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.opacity_btn)
        val invertBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.invert_colors_btn)
        val mirrorBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.mirror_btn)
        val countdownText = mFloatingWidget?.findViewById<TextView>(R.id.countdown_text)
        val recordBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.record_btn)
        val voicePaceBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.voice_pace_btn)
        val remoteBtn = mFloatingWidget?.findViewById<ImageButton>(R.id.remote_btn)

        var areAdvancedControlsVisible = false

        fun updateAdvancedControls() {
            advancedControlsContainer?.visibility = if (areAdvancedControlsVisible) View.VISIBLE else View.GONE
            advancedToggle?.text = if (areAdvancedControlsVisible) "Less" else "More"
            advancedToggle?.alpha = if (areAdvancedControlsVisible) 1f else 0.88f
        }
        updateAdvancedControls()

        advancedToggle?.setOnClickListener {
            areAdvancedControlsVisible = !areAdvancedControlsVisible
            updateAdvancedControls()
        }

        fun withPremiumAccess(featureName: String, source: String, action: () -> Unit) {
            if (isPremium) {
                action()
            } else {
                PremiumAccess.launchPaywall(this@FloatingPromptService, source, featureName)
            }
        }

        // Remote Control Setup
        fun stopRemoteControl() {
            isRemoteEnabled = false
            setButtonTint(remoteBtn, Color.WHITE)
            try {
                if (registrationListener != null) {
                    nsdManager?.unregisterService(registrationListener)
                    registrationListener = null
                }
                serverSocket?.close()
                remoteJob?.cancel()
            } catch (e: Exception) {}
        }

        fun startRemoteControl() {
            nsdManager = getSystemService(Context.NSD_SERVICE) as NsdManager
            try {
                serverSocket = ServerSocket(0) // Let system find an open port
                val port = serverSocket?.localPort ?: return
                
                val serviceInfo = NsdServiceInfo().apply {
                    serviceName = "ScriptQ Prompter"
                    serviceType = "_scriptq._tcp"
                    setPort(port)
                }
                
                registrationListener = object : NsdManager.RegistrationListener {
                    override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                        isRemoteEnabled = true
                        remoteJob = CoroutineScope(Dispatchers.IO).launch {
                            try {
                                while (isActive && isRemoteEnabled) {
                                    val client: Socket? = serverSocket?.accept()
                                    client?.let {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val reader = BufferedReader(InputStreamReader(it.getInputStream()))
                                                while (isActive) {
                                                    val message = reader.readLine() ?: break
                                                    withContext(Dispatchers.Main) {
                                                        when (message) {
                                                            "PLAY_PAUSE" -> playPauseBtn?.performClick()
                                                            "FASTER" -> speedUpBtn?.performClick()
                                                            "SLOWER" -> slowDownBtn?.performClick()
                                                            "RESET" -> resetBtn?.performClick()
                                                            else -> Unit
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {} finally {
                                                it.close()
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {}
                        }
                    }
                    override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) { stopRemoteControl() }
                    override fun onServiceUnregistered(arg0: NsdServiceInfo) { stopRemoteControl() }
                    override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {}
                }
                
                nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
                setButtonTint(remoteBtn, Color.GREEN)
                Toast.makeText(this@FloatingPromptService, "Remote Control Ready", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                stopRemoteControl()
                Toast.makeText(this@FloatingPromptService, "Failed to start Remote Control", Toast.LENGTH_SHORT).show()
            }
        }

        remoteBtn?.setOnClickListener {
            withPremiumAccess("Remote control", "prompter_remote") {
                if (isRemoteEnabled) {
                    stopRemoteControl()
                    Toast.makeText(this@FloatingPromptService, "Remote Control Disabled", Toast.LENGTH_SHORT).show()
                } else {
                    startRemoteControl()
                }
            }
        }

        // Voice Pacing Setup
        fun setupSpeechRecognizer() {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
                speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {
                        currentRmsdB = rmsdB
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onError(error: Int) {
                        if (isVoicePacing) {
                            // Restart listener on error (like timeout)
                            speechRecognizer?.startListening(speechIntent)
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        if (isVoicePacing) {
                            // Loop it
                            speechRecognizer?.startListening(speechIntent)
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
        setupSpeechRecognizer()

        // Start Voice Paced Scrolling Job
        fun startVoicePacing(scrollView: ScrollView) {
            scrollJob?.cancel()
            scrollJob = serviceScope.launch {
                while (isActive && isVoicePacing) {
                    if (currentRmsdB > 2.5f) {
                        // User is speaking, scroll at chosen speed
                        scrollView.smoothScrollBy(0, 2)
                        delay(scrollDelay)
                    } else {
                        // Silent or whispering, pause but loop fast
                        delay(50)
                    }
                }
            }
        }

        voicePaceBtn?.setOnClickListener {
            withPremiumAccess("Voice pacing", "prompter_voice_pacing") {
                if (ContextCompat.checkSelfPermission(this@FloatingPromptService, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    isVoicePacing = !isVoicePacing
                    if (isVoicePacing) {
                        // Turn ON Voice Pacing
                        setButtonTint(voicePaceBtn, Color.GREEN)
                        isPlaying = false
                        playPauseBtn?.setImageResource(android.R.drawable.ic_media_play)

                        speechRecognizer?.startListening(speechIntent)
                        startVoicePacing(scrollView!!)
                        Toast.makeText(this@FloatingPromptService, "Voice Pacing Enabled", Toast.LENGTH_SHORT).show()
                    } else {
                        // Turn OFF Voice Pacing
                        setButtonTint(voicePaceBtn, Color.WHITE)
                        speechRecognizer?.stopListening()
                        scrollJob?.cancel()
                        Toast.makeText(this@FloatingPromptService, "Voice Pacing Disabled", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@FloatingPromptService, "Microphone permission required for Voice Pacing", Toast.LENGTH_LONG).show()
                }
            }
        }

        // Update speed indicator
        fun updateSpeedText() {
            // Speed mapped so 200L is 1x and 10L is 20x
            val speedDisplay = (210 - scrollDelay) / 10
            speedText?.text = "${speedDisplay}x"
        }
        updateSpeedText()

        fun stopRecording() {
            if (isRecording) {
                isRecording = false
                recordBtn?.setImageResource(android.R.drawable.ic_btn_speak_now) // Default icon
                try {
                    mediaRecorder?.stop()
                    mediaRecorder?.release()
                    mediaRecorder = null
                    val durationMins = (System.currentTimeMillis() - recordStartTime) / 60000.0
                    val wordCount = textView?.text?.trim()?.split("\\s+".toRegex())?.filter { it.isNotEmpty() }?.size ?: 0
                    val wpm = if (durationMins > 0) (wordCount / durationMins).toInt() else 0
                    
                    Toast.makeText(this@FloatingPromptService, "Rehearsal saved! Avg WPM: $wpm", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this@FloatingPromptService, "Error stopping recording", Toast.LENGTH_SHORT).show()
                }
            }
        }

        recordBtn?.setOnClickListener {
            withPremiumAccess("Rehearsal recording", "prompter_recording") {
                if (isRecording) {
                    stopRecording()
                } else {
                    if (ContextCompat.checkSelfPermission(this@FloatingPromptService, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        val fileDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                        audioFilePath = "${fileDir?.absolutePath}/rehearsal_${System.currentTimeMillis()}.m4a"

                        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            MediaRecorder(this@FloatingPromptService)
                        } else {
                            @Suppress("DEPRECATION")
                            MediaRecorder()
                        }

                        try {
                            mediaRecorder?.apply {
                                setAudioSource(MediaRecorder.AudioSource.MIC)
                                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                setOutputFile(audioFilePath)
                                prepare()
                                start()
                            }
                            isRecording = true
                            recordStartTime = System.currentTimeMillis()
                            recordBtn.setImageResource(android.R.drawable.ic_media_pause)
                            Toast.makeText(this@FloatingPromptService, "Recording started", Toast.LENGTH_SHORT).show()
                        } catch (e: java.io.IOException) {
                            e.printStackTrace()
                            Toast.makeText(this@FloatingPromptService, "Recording failed to start", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@FloatingPromptService, "Microphone permission required", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        playPauseBtn?.setOnClickListener {
            if (isVoicePacing) {
                // Disable voice pacing on manual override
                isVoicePacing = false
                setButtonTint(voicePaceBtn, Color.WHITE)
                speechRecognizer?.stopListening()
            }
            if (!isPlaying) {
                // Start countdown then play
                isPlaying = true
                playPauseBtn.setImageResource(android.R.drawable.ic_media_pause)
                serviceScope.launch {
                    countdownText?.visibility = View.VISIBLE
                    for (i in 3 downTo 1) {
                        if (!isPlaying) break // Handle early cancellation
                        countdownText?.text = i.toString()
                        delay(1000)
                    }
                    countdownText?.visibility = View.GONE
                    if (isPlaying) {
                        startScrolling(scrollView!!)
                    }
                }
            } else {
                // Pause immediately
                isPlaying = false
                playPauseBtn.setImageResource(android.R.drawable.ic_media_play)
                scrollJob?.cancel()
                countdownText?.visibility = View.GONE
            }
        }

        speedUpBtn?.setOnClickListener {
            scrollDelay = (scrollDelay - 10L).coerceAtLeast(10L)
            updateSpeedText()
        }

        slowDownBtn?.setOnClickListener {
            scrollDelay = (scrollDelay + 10L).coerceAtMost(200L)
            updateSpeedText()
        }

        // 2. Reset scroll position button
        resetBtn?.setOnClickListener {
            scrollView?.scrollTo(0, 0)
        }

        // 3. Text size control
        increaseTextBtn?.setOnClickListener {
            textSizeSp = (textSizeSp + 2f).coerceAtMost(48f)
            textView?.textSize = textSizeSp
        }

        decreaseTextBtn?.setOnClickListener {
            textSizeSp = (textSizeSp - 2f).coerceAtLeast(12f)
            textView?.textSize = textSizeSp
        }

        // 4. Opacity slider or toggle
        opacityBtn?.setOnClickListener {
            opacityLevel = (opacityLevel + 1) % 3
            applyColors()
        }

        invertBtn?.setOnClickListener {
            isInverted = !isInverted
            applyColors()
        }

        mirrorBtn?.setOnClickListener {
            withPremiumAccess("Mirror mode", "prompter_mirror") {
                isMirrored = !isMirrored
                applyMirror()
            }
        }

        closeButton?.setOnClickListener {
            if (isRecording) stopRecording()
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
        if (isRecording) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
            } catch (e: Exception) {}
        }
        if (speechRecognizer != null) {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        if (mFloatingWidget != null) {
            mWindowManager?.removeView(mFloatingWidget)
            mFloatingWidget = null
        }
    }
}
