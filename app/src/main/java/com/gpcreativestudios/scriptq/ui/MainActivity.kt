package com.gpcreativestudios.scriptq.ui

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.gpcreativestudios.scriptq.data.AppDatabase
import com.gpcreativestudios.scriptq.data.Script
import com.gpcreativestudios.scriptq.data.ScriptRepository
import com.gpcreativestudios.scriptq.databinding.ActivityMainBinding
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val scriptViewModel: ScriptViewModel by viewModels {
        val database = AppDatabase.getDatabase(this)
        val repository = ScriptRepository(database.scriptDao())
        ScriptViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        val adapter = ScriptAdapter { script ->
            if (Settings.canDrawOverlays(this)) {
                val intent = Intent(this, FloatingPromptService::class.java)
                intent.putExtra("SCRIPT_TEXT", script.textContent)
                startService(intent)
            } else {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }
        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(this)

        scriptViewModel.allScripts.observe(this) { scripts ->
            scripts?.let { adapter.submitList(it) }
        }

        binding.fab.setOnClickListener {
            val intent = android.content.Intent(this, ScriptEditorActivity::class.java)
            startActivity(intent)
        }
    }
}
