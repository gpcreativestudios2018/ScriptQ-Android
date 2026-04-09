package com.gpcreativestudios.scriptq.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gpcreativestudios.scriptq.R
import com.gpcreativestudios.scriptq.databinding.ActivityOverlayAccessBinding

class OverlayAccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOverlayAccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOverlayAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.midnight_900)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.midnight_900)

        binding.buttonOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }
        binding.buttonOverlayInfo.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Overlay access")
                .setMessage("Android may open a general list instead of ScriptQ directly. That is normal. Choose ScriptQ from the list, then turn on Allow display over other apps.")
                .setPositiveButton("OK", null)
                .show()
        }

        binding.buttonCheckAgain.setOnClickListener {
            finishIfEnabled()
        }

        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun refreshUi() {
        val enabled = Settings.canDrawOverlays(this)
        binding.textStatus.text = if (enabled) "Overlay access is on." else "Overlay access is off."
        binding.textInlineStatus.text =
            if (enabled) {
                "ScriptQ can now float the prompter over other apps. Tap Check again to continue."
            } else {
                "If Android opens a generic list instead of ScriptQ directly, choose ScriptQ manually from that list."
            }
    }

    private fun finishIfEnabled() {
        if (Settings.canDrawOverlays(this)) {
            setResult(Activity.RESULT_OK)
            finish()
        } else {
            binding.textInlineStatus.text =
                "ScriptQ still does not have overlay access. In Android's list, choose ScriptQ and enable Allow display over other apps."
        }
    }
}
