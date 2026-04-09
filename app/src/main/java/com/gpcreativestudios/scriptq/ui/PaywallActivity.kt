package com.gpcreativestudios.scriptq.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.gpcreativestudios.scriptq.databinding.ActivityPaywallBinding
import com.revenuecat.purchases.Package

class PaywallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaywallBinding
    private val billingViewModel: BillingViewModel by viewModels()
    private var activePackage: Package? = null
    private var primaryPackage: Package? = null
    private var secondaryPackage: Package? = null
    private var isSecondaryPackageAvailable = false

    private fun humanizePackageName(pkg: Package): String {
        return when (pkg.packageType.name) {
            "ANNUAL" -> "Annual Pro"
            "MONTHLY" -> "Monthly Pro"
            "WEEKLY" -> "Weekly Pro"
            "LIFETIME" -> "Lifetime Pro"
            else -> pkg.product.title.substringBefore("(").trim().ifBlank { "ScriptQ Pro" }
        }
    }

    private fun packageDetail(pkg: Package): String {
        return when (pkg.packageType.name) {
            "ANNUAL" -> "Best value for creators who use ScriptQ every week."
            "MONTHLY" -> "Flexible access while you dial in your production flow."
            "WEEKLY" -> "Short-term access for a sprint or launch window."
            "LIFETIME" -> "One payment, full unlocked studio."
            else -> "Full access to the premium teleprompter studio."
        }
    }

    private fun packagePriority(pkg: Package): Int {
        return when (pkg.packageType.name) {
            "ANNUAL" -> 0
            "MONTHLY" -> 1
            "LIFETIME" -> 2
            "WEEKLY" -> 3
            else -> 4
        }
    }

    private fun selectPackage(selected: Package) {
        activePackage = selected
        binding.textViewPrice.text = "Selected: ${humanizePackageName(selected)} - ${selected.product.price.formatted}"
        binding.buttonBuy.isEnabled = true
        binding.buttonBuy.text = "Unlock ${humanizePackageName(selected)}"

        val isPrimarySelected = selected == primaryPackage
        if (isPrimarySelected) {
            binding.planPrimaryCard.strokeWidth = 2
            binding.planPrimaryCard.strokeColor = getColor(com.gpcreativestudios.scriptq.R.color.accent_primary)
            binding.planSecondaryCard.strokeWidth = 1
            binding.planSecondaryCard.strokeColor = getColor(com.gpcreativestudios.scriptq.R.color.glass_stroke_soft)
        } else {
            binding.planPrimaryCard.strokeWidth = 1
            binding.planPrimaryCard.strokeColor = getColor(com.gpcreativestudios.scriptq.R.color.glass_stroke_soft)
            binding.planSecondaryCard.strokeWidth = 2
            binding.planSecondaryCard.strokeColor = getColor(com.gpcreativestudios.scriptq.R.color.accent_primary)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaywallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, com.gpcreativestudios.scriptq.R.color.midnight_900)
        window.navigationBarColor = ContextCompat.getColor(this, com.gpcreativestudios.scriptq.R.color.midnight_900)

        val requestedFeature = intent.getStringExtra(PremiumAccess.EXTRA_FEATURE_NAME)
        if (!requestedFeature.isNullOrBlank()) {
            binding.textViewTitle.text = "Unlock $requestedFeature."
            binding.textRequestedFeature.text = "$requestedFeature • Pro"
            binding.textViewSubtitle.text = "$requestedFeature is part of ScriptQ Pro. Choose the plan that matches how often you rehearse, prompt, and publish."
        }

        binding.buttonClose.setOnClickListener { finish() }
        binding.buttonMaybeLater.setOnClickListener { finish() }

        billingViewModel.offerings.observe(this) { offerings ->
            val packages = offerings?.current?.availablePackages.orEmpty().sortedBy(::packagePriority)
            primaryPackage = packages.firstOrNull { it.packageType.name == "ANNUAL" } ?: packages.firstOrNull()
            secondaryPackage = packages.firstOrNull { it.packageType.name == "MONTHLY" }
            val secondary = secondaryPackage
            isSecondaryPackageAvailable = secondary != null

            primaryPackage?.let { pkg ->
                binding.textPrimaryPlanName.text = humanizePackageName(pkg)
                binding.textPrimaryPlanDetail.text = packageDetail(pkg)
                binding.textPrimaryPlanPrice.text = pkg.product.price.formatted
                binding.textPrimaryPlanTag.text =
                    if (pkg.packageType.name == "ANNUAL") "BEST VALUE" else "RECOMMENDED"

                binding.planPrimaryCard.setOnClickListener { selectPackage(pkg) }

                if (secondary != null) {
                    binding.planSecondaryCard.visibility = View.VISIBLE
                    binding.planSecondaryCard.alpha = 1f
                    binding.planSecondaryCard.isClickable = true
                    binding.textSecondaryPlanName.text = humanizePackageName(secondary)
                    binding.textSecondaryPlanDetail.text = packageDetail(secondary)
                    binding.textSecondaryPlanPrice.text = secondary.product.price.formatted
                    binding.textSecondaryPlanTag.text =
                        if (secondary.packageType.name == "MONTHLY") "MOST FLEXIBLE" else "ALTERNATE"
                    binding.planSecondaryCard.setOnClickListener { selectPackage(secondary) }
                } else {
                    binding.planSecondaryCard.visibility = View.VISIBLE
                    binding.planSecondaryCard.alpha = 0.72f
                    binding.planSecondaryCard.strokeWidth = 1
                    binding.planSecondaryCard.strokeColor = getColor(com.gpcreativestudios.scriptq.R.color.glass_stroke_soft)
                    binding.textSecondaryPlanName.text = "Monthly Pro"
                    binding.textSecondaryPlanDetail.text = "Monthly billing is still missing from the active Play Console + RevenueCat setup."
                    binding.textSecondaryPlanPrice.text = "Setup needed"
                    binding.textSecondaryPlanTag.text = "MONTHLY INFO"
                    binding.planSecondaryCard.setOnClickListener {
                        Toast.makeText(
                            this,
                            "Monthly billing still needs Play Console and RevenueCat setup for this build.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                selectPackage(pkg)
                binding.buttonBuy.isEnabled = true
                binding.textBillingNote.text =
                    if (secondary == null) {
                        "Annual is live. Monthly is still missing from the active Play Console + RevenueCat offering for this build."
                    } else {
                        "Choose the plan that fits your workflow. Monthly and annual are surfaced first when available."
                    }
            } ?: run {
                binding.textViewPrice.text = "Offerings unavailable"
                binding.buttonBuy.isEnabled = false
                binding.buttonBuy.text = "Unlock Premium"
                binding.planSecondaryCard.visibility = View.VISIBLE
                binding.planSecondaryCard.alpha = 0.72f
                binding.textSecondaryPlanName.text = "Monthly Pro"
                binding.textSecondaryPlanDetail.text = "No monthly package is available from the current RevenueCat offering."
                binding.textSecondaryPlanPrice.text = "Setup needed"
                binding.textSecondaryPlanTag.text = "MONTHLY INFO"
                binding.textBillingNote.text = "No purchasable plans are available in the current RevenueCat offering."
            }
        }

        billingViewModel.billingMessage.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                binding.textViewPrice.text = message
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
