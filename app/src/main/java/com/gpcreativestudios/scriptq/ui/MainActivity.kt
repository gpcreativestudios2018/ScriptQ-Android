package com.gpcreativestudios.scriptq.ui

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.graphics.drawable.ColorDrawable
import android.provider.Settings
import android.view.View
import android.widget.Toast
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView
import com.gpcreativestudios.scriptq.data.AppDatabase
import com.gpcreativestudios.scriptq.data.Script
import com.gpcreativestudios.scriptq.data.ScriptRepository
import com.gpcreativestudios.scriptq.databinding.ActivityMainBinding
import com.gpcreativestudios.scriptq.BuildConfig
import com.google.android.material.button.MaterialButton
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.LoadAdError

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
    private var mInterstitialAd: InterstitialAd? = null
    private var pendingPromptScriptText: String? = null
    private var pendingPromptScriptTitle: String? = null

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == android.app.Activity.RESULT_OK && Settings.canDrawOverlays(this)) {
                startPendingPromptIfAvailable()
            } else if (pendingPromptScriptText != null) {
                showOverlayStillDisabledDialog()
            }
        }

    private fun withPremiumAccess(featureName: String, source: String, action: () -> Unit) {
        if (isPremium) {
            action()
        } else {
            PremiumAccess.launchPaywall(this, source, featureName)
        }
    }

    private fun configureBannerAd() {
        if (BuildConfig.ADMOB_BANNER_ID.isBlank()) {
            binding.adView.visibility = View.GONE
            return
        }

        MobileAds.initialize(this) {}
        binding.adView.loadAd(AdRequest.Builder().build())
    }

    private fun showInfoDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openOverlayAccessFlow(script: Script) {
        pendingPromptScriptText = script.textContent
        pendingPromptScriptTitle = script.title
        overlayPermissionLauncher.launch(Intent(this, OverlayAccessActivity::class.java))
    }

    private fun showOverlayStillDisabledDialog() {
        AlertDialog.Builder(this)
            .setTitle("Overlay still off")
            .setMessage(
                "ScriptQ still does not have overlay access.\n\n" +
                    "Tap Continue and follow the in-app steps to choose ScriptQ from Android's Display over other apps list."
            )
            .setPositiveButton("Continue") { _, _ ->
                overlayPermissionLauncher.launch(Intent(this, OverlayAccessActivity::class.java))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startPromptService(scriptText: String) {
        val intent = Intent(this, FloatingPromptService::class.java).apply {
            putExtra("SCRIPT_TEXT", scriptText)
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun showPrompterReadyDialog(scriptTitle: String?) {
        val dialogView = layoutInflater.inflate(com.gpcreativestudios.scriptq.R.layout.dialog_prompter_ready, null)
        dialogView.findViewById<TextView>(com.gpcreativestudios.scriptq.R.id.textPrompterTitle).text =
            if (scriptTitle.isNullOrBlank()) "Your script is live." else "\"$scriptTitle\" is live."

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        dialogView.findViewById<MaterialButton>(com.gpcreativestudios.scriptq.R.id.buttonOpenTargetApp).setOnClickListener {
            dialog.dismiss()
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(homeIntent)
        }

        dialogView.findViewById<MaterialButton>(com.gpcreativestudios.scriptq.R.id.buttonBackToLibrary).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(com.gpcreativestudios.scriptq.R.id.buttonStopPrompter).setOnClickListener {
            stopService(Intent(this, FloatingPromptService::class.java).apply {
                action = FloatingPromptService.ACTION_STOP
            })
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun startPendingPromptIfAvailable() {
        val scriptText = pendingPromptScriptText
        val scriptTitle = pendingPromptScriptTitle
        if (scriptText != null) {
            startPromptService(scriptText)
            showPrompterReadyDialog(scriptTitle)
        }
        pendingPromptScriptText = null
        pendingPromptScriptTitle = null
    }

    private fun loadInterstitial() {
        if (isPremium || BuildConfig.ADMOB_BANNER_ID.isBlank()) return
        val adRequest = AdRequest.Builder().build()
        // Using a test ID for development. In production, use the real ID from local.properties if defined.
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
            }
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
            }
        })
    }

    private fun handlePromptSessionStart(script: Script) {
        val prefs = getSharedPreferences("ScriptQPrefs", android.content.Context.MODE_PRIVATE)
        var promptSessions = prefs.getInt("prompt_sessions", 0)
        
        if (!isPremium && promptSessions >= 2 && mInterstitialAd != null) {
            mInterstitialAd?.show(this)
            prefs.edit().putInt("prompt_sessions", 0).apply()
            loadInterstitial() // pre-load next one
        } else {
            if (!isPremium) {
                prefs.edit().putInt("prompt_sessions", promptSessions + 1).apply()
            }
        }

        if (Settings.canDrawOverlays(this)) {
            pendingPromptScriptText = null
            pendingPromptScriptTitle = null
            startPromptService(script.textContent)
            showPrompterReadyDialog(script.title)
        } else {
            openOverlayAccessFlow(script)
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(com.gpcreativestudios.scriptq.R.menu.menu_main, menu)
        
        val searchItem = menu.findItem(com.gpcreativestudios.scriptq.R.id.action_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = "Search scripts..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true
            override fun onQueryTextChange(newText: String?): Boolean {
                scriptViewModel.setSearchQuery(newText.orEmpty())
                return true
            }
        })
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            com.gpcreativestudios.scriptq.R.id.sort_created -> {
                scriptViewModel.setSortOrder(SortOrder.CREATED)
                true
            }
            com.gpcreativestudios.scriptq.R.id.sort_updated -> {
                scriptViewModel.setSortOrder(SortOrder.UPDATED)
                true
            }
            com.gpcreativestudios.scriptq.R.id.sort_title -> {
                scriptViewModel.setSortOrder(SortOrder.TITLE)
                true
            }
            com.gpcreativestudios.scriptq.R.id.action_remote -> {
                withPremiumAccess("Remote control", "main_toolbar_remote") {
                    startActivity(Intent(this, RemoteControlActivity::class.java))
                }
                true
            }
            com.gpcreativestudios.scriptq.R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        pendingPromptScriptText = savedInstanceState?.getString("pending_prompt_script_text")
        pendingPromptScriptTitle = savedInstanceState?.getString("pending_prompt_script_title")
        
        val prefs = getSharedPreferences("ScriptQPrefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("has_seen_onboarding", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        window.statusBarColor = ContextCompat.getColor(this, com.gpcreativestudios.scriptq.R.color.midnight_900)
        window.navigationBarColor = ContextCompat.getColor(this, com.gpcreativestudios.scriptq.R.color.midnight_900)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        binding.toolbar.overflowIcon = ContextCompat.getDrawable(this, com.gpcreativestudios.scriptq.R.drawable.ic_toolbar_overflow)
        binding.toolbar.overflowIcon?.setTint(ContextCompat.getColor(this, com.gpcreativestudios.scriptq.R.color.text_primary))
        binding.buttonPromptInfo.setOnClickListener {
            showInfoDialog(
                "Prompt",
                "Prompt launches the floating teleprompter so your script can stay on screen over Camera, Meet, or another app."
            )
        }
        binding.buttonNewScriptInfo.setOnClickListener {
            showInfoDialog(
                "New Script",
                "Use New Script to create a fresh script in your library. Tap a script card later to edit it."
            )
        }

        configureBannerAd()

        billingViewModel.isProActive.observe(this) { proActive ->
            isPremium = proActive
            PremiumAccess.cachePremiumStatus(this, proActive)
            if (isPremium || BuildConfig.ADMOB_BANNER_ID.isBlank()) {
                binding.adView.visibility = View.GONE
            } else {
                binding.adView.visibility = View.VISIBLE
                loadInterstitial()
            }
        }

        val adapter = ScriptAdapter(
            onItemClick = { script ->
                val intent = Intent(this, ScriptEditorActivity::class.java)
                intent.putExtra("SCRIPT_ID", script.id)
                intent.putExtra("SCRIPT_TITLE", script.title)
                intent.putExtra("SCRIPT_CONTENT", script.textContent)
                intent.putExtra("SCRIPT_CREATED_AT", script.createdAt)
                startActivity(intent)
            },
            onItemLongClick = { anchor, script ->
                showContextMenu(anchor, script)
                true
            },
            onFavoriteClick = { script ->
                val updatedScript = script.copy(isFavorite = !script.isFavorite)
                scriptViewModel.insert(updatedScript)
                val status = if (updatedScript.isFavorite) "Added to Favorites" else "Removed from Favorites"
                Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
            },
            onPromptClick = { script ->
                handlePromptSessionStart(script)
            }
        )
        binding.recyclerview.adapter = adapter
        binding.recyclerview.layoutManager = LinearLayoutManager(this)

        // Implement Swipe-to-Delete
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val script = adapter.currentList[position]
                
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Delete Script")
                    .setMessage("Are you sure you want to delete '${script.title}'?")
                    .setPositiveButton("Delete") { _, _ ->
                        scriptViewModel.delete(script)
                        Toast.makeText(this@MainActivity, "Script deleted", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel") { _, _ ->
                        adapter.notifyItemChanged(position)
                    }
                    .setOnCancelListener {
                        adapter.notifyItemChanged(position)
                    }
                    .show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerview)

        scriptViewModel.allScripts.observe(this) { scripts ->
            scripts?.let { 
                adapter.submitList(it)
                scriptCount = it.size
                binding.emptyStateView.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("pending_prompt_script_text", pendingPromptScriptText)
        outState.putString("pending_prompt_script_title", pendingPromptScriptTitle)
    }

    private fun showContextMenu(anchor: View, script: Script) {
        val popup = PopupMenu(
            ContextThemeWrapper(this, com.gpcreativestudios.scriptq.R.style.ThemeOverlay_ScriptQ_PopupMenu),
            anchor
        )
        popup.menu.add("Edit")
        popup.menu.add("Delete")
        popup.menu.add("Duplicate")
        popup.menu.add("Share")
        popup.menu.add("Launch Prompter")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Edit" -> {
                    val intent = Intent(this, ScriptEditorActivity::class.java)
                    intent.putExtra("SCRIPT_ID", script.id)
                    intent.putExtra("SCRIPT_TITLE", script.title)
                    intent.putExtra("SCRIPT_CONTENT", script.textContent)
                    intent.putExtra("SCRIPT_CREATED_AT", script.createdAt)
                    startActivity(intent)
                }
                "Delete" -> {
                    AlertDialog.Builder(this)
                        .setTitle("Delete Script")
                        .setMessage("Are you sure you want to delete '${script.title}'?")
                        .setPositiveButton("Delete") { _, _ ->
                            scriptViewModel.delete(script)
                            Toast.makeText(this, "Script deleted", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                "Duplicate" -> {
                    val duplicatedScript = Script(
                        title = "${script.title} (Copy)",
                        textContent = script.textContent
                    )
                    scriptViewModel.insert(duplicatedScript)
                    Toast.makeText(this, "Script duplicated", Toast.LENGTH_SHORT).show()
                }
                "Share" -> {
                    val shareIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TITLE, script.title)
                        putExtra(Intent.EXTRA_TEXT, "${script.title}\n\n${script.textContent}")
                        type = "text/plain"
                    }
                    startActivity(Intent.createChooser(shareIntent, "Share Script via"))
                }
                "Launch Prompter" -> {
                    handlePromptSessionStart(script)
                }
            }
            true
        }
        popup.show()
    }
}
