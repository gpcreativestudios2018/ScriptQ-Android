package com.gpcreativestudios.scriptq.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.gpcreativestudios.scriptq.databinding.ActivityPaywallBinding
import com.revenuecat.purchases.Package

class PaywallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaywallBinding
    private val billingViewModel: BillingViewModel by viewModels()
    private var activePackage: Package? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaywallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        billingViewModel.offerings.observe(this) { offerings ->
            activePackage = offerings?.current?.availablePackages?.firstOrNull()
            activePackage?.let { pkg ->
                binding.textViewPrice.text = "Price: ${pkg.product.price.formatted}"
                binding.buttonBuy.isEnabled = true
            } ?: run {
                binding.textViewPrice.text = "Offerings unavailable"
                binding.buttonBuy.isEnabled = false
            }
        }

        binding.buttonBuy.setOnClickListener {
            activePackage?.let { pkg ->
                billingViewModel.purchasePackage(this, pkg) {
                    Toast.makeText(this, "Welcome to Premium!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
        
        // Initially disable button until offerings are loaded
        binding.buttonBuy.isEnabled = false
    }
}
