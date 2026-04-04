package com.gpcreativestudios.scriptq.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableCornerRadius
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.getCustomerInfoWith

class BillingViewModel : ViewModel() {

    private val _offerings = MutableLiveData<Offerings?>()
    val offerings: LiveData<Offerings?> = _offerings

    private val _isProActive = MutableLiveData<Boolean>(false)
    val isProActive: LiveData<Boolean> = _isProActive

    init {
        fetchOfferings()
        updateCustomerInfo()
    }

    private fun fetchOfferings() {
        Purchases.sharedInstance.getOfferingsWith(
            onError = { /* Log error appropriately */ },
            onSuccess = { offerings ->
                _offerings.postValue(offerings)
            }
        )
    }

    fun updateCustomerInfo() {
        Purchases.sharedInstance.getCustomerInfoWith(
            onError = { /* Log error appropriately */ },
            onSuccess = { customerInfo ->
                // "pro" is used here as a placeholder for the entitlement ID.
                _isProActive.postValue(customerInfo.entitlements["pro"]?.isActive == true)
            }
        )
    }
}
