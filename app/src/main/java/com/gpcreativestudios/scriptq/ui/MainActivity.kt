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
    private val billingViewModel: BillingViewModel by viewModels()
    private var isPremium = false
    private var scriptCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MobileAds.initialize(this) {}
        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        billingViewModel.isProActive.observe(this) { proActive ->
            isPremium = proActive
            if (isPremium) {
                binding.adView.visibility = View.GONE
            } else {
                binding.adView.visibility = View.VISIBLE
            }
        }

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
            scripts?.let { 
                adapter.submitList(it)
                scriptCount = it.size
            }
        }

        binding.fab.setOnClickListener {
            if (isPremium || scriptCount < 3) {
                val intent = Intent(this, ScriptEditorActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, PaywallActivity::class.java)
                startActivity(intent)
                Toast.makeText(this, "Upgrade to Premium for unlimited scripts", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
