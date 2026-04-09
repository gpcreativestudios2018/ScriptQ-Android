package com.gpcreativestudios.scriptq.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ViewFlipper
import android.widget.Toast
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.gpcreativestudios.scriptq.R

class OnboardingActivity : AppCompatActivity() {
    private var awaitingOverlayPermission = false

    private fun updateStepUi(viewFlipper: ViewFlipper, nextButton: Button, skipButton: Button) {
        val isPermissionStep = viewFlipper.displayedChild == 2
        nextButton.text = if (isPermissionStep) "Enable Overlay" else "Next"
        skipButton.visibility = if (isPermissionStep) View.VISIBLE else View.INVISIBLE
        val activeDot = android.R.drawable.presence_online
        val inactiveDot = android.R.drawable.presence_invisible
        findViewById<View>(R.id.dot1).setBackgroundResource(if (viewFlipper.displayedChild == 0) activeDot else inactiveDot)
        findViewById<View>(R.id.dot2).setBackgroundResource(if (viewFlipper.displayedChild == 1) activeDot else inactiveDot)
        findViewById<View>(R.id.dot3).setBackgroundResource(if (viewFlipper.displayedChild == 2) activeDot else inactiveDot)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val viewFlipper = findViewById<ViewFlipper>(R.id.viewFlipper)
        val nextButton = findViewById<Button>(R.id.nextButton)
        val skipButton = findViewById<Button>(R.id.skipButton)

        updateStepUi(viewFlipper, nextButton, skipButton)

        nextButton.setOnClickListener {
            if (viewFlipper.displayedChild == 2) {
                // Request Mic permission first
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1235)
                } else {
                    checkOverlayPermission()
                }
            } else {
                viewFlipper.showNext()
                updateStepUi(viewFlipper, nextButton, skipButton)
            }
        }

        skipButton.setOnClickListener {
            finishOnboarding()
        }
    }

    override fun onResume() {
        super.onResume()
        if (awaitingOverlayPermission && Settings.canDrawOverlays(this)) {
            awaitingOverlayPermission = false
            finishOnboarding()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1235) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                checkOverlayPermission()
            } else {
                Toast.makeText(this, "Microphone permission skipped. You can still use basic prompting.", Toast.LENGTH_LONG).show()
                checkOverlayPermission()
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            awaitingOverlayPermission = true
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1234)
        } else {
            awaitingOverlayPermission = false
            finishOnboarding()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1234) {
            if (Settings.canDrawOverlays(this)) {
                awaitingOverlayPermission = false
                finishOnboarding()
            } else {
                Toast.makeText(this, "Overlay access can be enabled later from Settings.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun finishOnboarding() {
        val prefs = getSharedPreferences("ScriptQPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("has_seen_onboarding", true).apply()

        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(launchIntent)
        finish()
    }
}
