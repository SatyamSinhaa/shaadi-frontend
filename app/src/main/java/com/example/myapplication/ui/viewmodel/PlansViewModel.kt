package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.Plan
import com.example.myapplication.data.model.Subscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PlansViewModel : ViewModel() {
    private val _plans = MutableStateFlow<List<Plan>>(emptyList())
    val plans: StateFlow<List<Plan>> = _plans

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    private val _subscriptionHistory = MutableStateFlow<List<Subscription>>(emptyList())
    val subscriptionHistory: StateFlow<List<Subscription>> = _subscriptionHistory
    
    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading

    private val _purchaseState = MutableStateFlow<String?>(null)
    val purchaseState: StateFlow<String?> = _purchaseState

    init {
        fetchPlans()
    }

    fun fetchPlans() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response = RetrofitClient.apiService.getAllPlans()
                if (response.isSuccessful) {
                    val allPlans = response.body() ?: emptyList()
                    // Filter only published plans
                    _plans.value = allPlans.filter { it.isPublished }
                } else {
                    _error.value = "Failed to fetch plans: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                _error.value = "Exception: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchSubscriptionHistory(userId: Int) {
        viewModelScope.launch {
            _historyLoading.value = true
            try {
                val response = RetrofitClient.apiService.getSubscriptionHistory(userId)
                if (response.isSuccessful) {
                    _subscriptionHistory.value = response.body() ?: emptyList()
                } else {
                    // handle error silently or show toast if needed
                }
            } catch (e: Exception) {
                // handle exception
            } finally {
                _historyLoading.value = false
            }
        }
    }

    fun purchaseSubscription(userId: Int, planId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _purchaseState.value = null
            try {
                val body = mapOf("planId" to planId)
                val response = RetrofitClient.apiService.purchaseSubscription(userId, body)
                if (response.isSuccessful) {
                    _purchaseState.value = "Success"
                } else {
                    _purchaseState.value = "Failed: ${response.message()}"
                }
            } catch (e: Exception) {
                _purchaseState.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetPurchaseState() {
        _purchaseState.value = null
    }
    
    fun clearHistory() {
        _subscriptionHistory.value = emptyList()
    }
}
