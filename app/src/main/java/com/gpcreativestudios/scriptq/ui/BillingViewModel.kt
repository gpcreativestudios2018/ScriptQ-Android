package com.gpcreativestudios.scriptq.ui

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.purchasePackageWith

class BillingViewModel : ViewModel() {
    companion object {
        private const val TAG = "BillingViewModel"
        private const val PRO_ENTITLEMENT_ID = "pro"
    }

    private val _offerings = MutableLiveData<Offerings?>()
    val offerings: LiveData<Offerings?> = _offerings

    private val _isProActive = MutableLiveData<Boolean>(false)
    val isProActive: LiveData<Boolean> = _isProActive

    private val _billingMessage = MutableLiveData<String?>()
    val billingMessage: LiveData<String?> = _billingMessage

    init {
        fetchOfferings()
        updateCustomerInfo()
    }

    private inline fun withPurchasesClient(action: () -> Unit) {
        runCatching { action() }
            .onFailure { error ->
                Log.e(TAG, "RevenueCat is unavailable in this build", error)
                _billingMessage.postValue("Premium is unavailable in this build.")
                _offerings.postValue(null)
                _isProActive.postValue(false)
            }
    }

    private fun fetchOfferings() {
        withPurchasesClient {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                Log.e(TAG, "Failed to fetch offerings: ${error.message}")
                    _billingMessage.postValue("Unable to load subscription options right now.")
                    _offerings.postValue(null)
                },
                onSuccess = { offerings ->
                    _offerings.postValue(offerings)
                    _billingMessage.postValue(null)
                }
            )
        }
    }

    fun updateCustomerInfo() {
        withPurchasesClient {
            Purchases.sharedInstance.getCustomerInfoWith(
                onError = { error ->
                    Log.e(TAG, "Failed to fetch customer info: ${error.message}")
                    _billingMessage.postValue("Unable to confirm Premium status right now.")
                },
                onSuccess = { customerInfo ->
                    _isProActive.postValue(customerInfo.entitlements[PRO_ENTITLEMENT_ID]?.isActive == true)
                    _billingMessage.postValue(null)
                }
            )
        }
    }

    fun purchasePackage(activity: Activity, packageToPurchase: Package, onComplete: () -> Unit) {
        withPurchasesClient {
            Purchases.sharedInstance.purchasePackageWith(
                activity,
                packageToPurchase,
                onError = { error, userCancelled ->
                    if (!userCancelled) {
                        Log.e(TAG, "Purchase failed: ${error.message}")
                        _billingMessage.postValue(error.message)
                        Toast.makeText(activity, "Error: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onSuccess = { _, customerInfo ->
                    _isProActive.postValue(customerInfo.entitlements[PRO_ENTITLEMENT_ID]?.isActive == true)
                    _billingMessage.postValue(null)
                    onComplete()
                }
            )
        }
    }
}
