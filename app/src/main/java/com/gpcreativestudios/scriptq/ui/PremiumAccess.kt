package com.gpcreativestudios.scriptq.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast

object PremiumAccess {
    private const val PREFS_NAME = "ScriptQPrefs"
    private const val KEY_IS_PREMIUM_CACHED = "is_premium_cached"

    const val EXTRA_SOURCE = "premium_source"
    const val EXTRA_FEATURE_NAME = "premium_feature_name"

    fun cachePremiumStatus(context: Context, isPremium: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_PREMIUM_CACHED, isPremium)
            .apply()
    }

    fun isPremiumCached(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_PREMIUM_CACHED, false)
    }

    fun launchPaywall(context: Context, source: String, featureName: String) {
        val intent = Intent(context, PaywallActivity::class.java).apply {
            putExtra(EXTRA_SOURCE, source)
            putExtra(EXTRA_FEATURE_NAME, featureName)
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
        context.startActivity(intent)
        Toast.makeText(context, "$featureName is part of ScriptQ Pro.", Toast.LENGTH_SHORT).show()
    }
}
