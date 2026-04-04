package com.gpcreativestudios.scriptq

import android.app.Application
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initializing RevenueCat with a placeholder key.
        // In a production app, the real API key should be securely stored.
        Purchases.configure(
            PurchasesConfiguration.Builder(this, "goog_your_placeholder_api_key_here").build()
        )
    }
}
