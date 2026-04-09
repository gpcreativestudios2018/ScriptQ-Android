package com.gpcreativestudios.scriptq

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.google.android.material.color.DynamicColors
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class App : Application() {
    companion object {
        private const val TAG = "ScriptQApp"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Apply Material 3 Dynamic Colors
        DynamicColors.applyToActivitiesIfAvailable(this)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        when (prefs.getString("theme_preference", "system")) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        if (BuildConfig.REVENUECAT_API_KEY.isBlank()) {
            Log.w(TAG, "RevenueCat API key is missing. Premium features will be unavailable in this build.")
            return
        }

        Purchases.configure(
            PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build()
        )
    }
}
