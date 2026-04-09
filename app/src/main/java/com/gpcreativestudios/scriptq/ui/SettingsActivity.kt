package com.gpcreativestudios.scriptq.ui

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.gpcreativestudios.scriptq.R
import com.gpcreativestudios.scriptq.databinding.ActivitySettingsBinding
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.restorePurchasesWith
import android.provider.Settings
import android.content.Intent

class SettingsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SettingsActivity"
    }

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        window.statusBarColor = ContextCompat.getColor(this, R.color.midnight_900)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.midnight_900)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)

            val themePref = findPreference<ListPreference>("theme_preference")
            themePref?.setOnPreferenceChangeListener { _, newValue ->
                when (newValue as String) {
                    "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                true
            }

            val overlayAccessPref = findPreference<Preference>("overlay_access")
            overlayAccessPref?.summary =
                if (Settings.canDrawOverlays(requireContext())) {
                    "Enabled for ScriptQ. Tap to review the in-app overlay access guide."
                } else {
                    "Required for prompting over other apps. Tap for ScriptQ's in-app overlay access guide."
                }
            overlayAccessPref?.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), OverlayAccessActivity::class.java))
                true
            }

            val restorePurchasesPref = findPreference<Preference>("restore_purchases")
            restorePurchasesPref?.setOnPreferenceClickListener {
                runCatching {
                    Purchases.sharedInstance.restorePurchasesWith(
                        onSuccess = { customerInfo ->
                            if (customerInfo.entitlements["pro"]?.isActive == true) {
                                Toast.makeText(context, "Purchases restored successfully. Welcome back to Premium!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "No active Premium subscription found.", Toast.LENGTH_LONG).show()
                            }
                        },
                        onError = { error ->
                            Toast.makeText(context, "Restore failed: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    )
                }.onFailure { error ->
                    Log.e(TAG, "Restore purchases unavailable", error)
                    Toast.makeText(context, "Premium is unavailable in this build.", Toast.LENGTH_LONG).show()
                }
                true
            }

        }

        override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
            listView.setPadding(0, 12, 0, 24)
            listView.clipToPadding = false
            listView.overScrollMode = android.view.View.OVER_SCROLL_NEVER
            listView.addOnChildAttachStateChangeListener(object : RecyclerView.OnChildAttachStateChangeListener {
                override fun onChildViewAttachedToWindow(child: android.view.View) {
                    tintPreferenceRow(child)
                }

                override fun onChildViewDetachedFromWindow(view: android.view.View) = Unit
            })
            listView.post {
                for (index in 0 until listView.childCount) {
                    tintPreferenceRow(listView.getChildAt(index))
                }
            }
        }

        override fun onResume() {
            super.onResume()
            findPreference<Preference>("overlay_access")?.summary =
                if (Settings.canDrawOverlays(requireContext())) {
                    "Enabled for ScriptQ. Tap to review the in-app overlay access guide."
                } else {
                    "Required for prompting over other apps. Tap for ScriptQ's in-app overlay access guide."
                }
        }

        private fun tintPreferenceRow(child: android.view.View) {
            tintTextViewsRecursively(child)
        }

        private fun tintTextViewsRecursively(view: android.view.View) {
            when (view) {
                is android.widget.TextView -> {
                    val titleColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
                    val secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                    val tertiaryColor = ContextCompat.getColor(requireContext(), R.color.text_tertiary)
                    val text = view.text?.toString().orEmpty()
                    view.alpha = 1f
                    view.setShadowLayer(0f, 0f, 0f, android.graphics.Color.TRANSPARENT)
                    view.setTextColor(
                        when {
                            text.equals("Appearance", true) ||
                                text.equals("Prompter Defaults", true) ||
                                text.equals("Premium", true) ||
                                text.equals("About", true) -> tertiaryColor
                            view.id == android.R.id.summary -> secondaryColor
                            else -> titleColor
                        }
                    )
                }
                is android.view.ViewGroup -> {
                    for (index in 0 until view.childCount) {
                        tintTextViewsRecursively(view.getChildAt(index))
                    }
                }
            }
        }
    }
}
