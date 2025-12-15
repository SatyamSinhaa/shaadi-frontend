package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.ApiService
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class SearchViewModel(private val apiService: ApiService = RetrofitClient.apiService) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun searchUsers(
        minAge: Int? = null,
        maxAge: Int? = null,
        name: String? = null,
        location: String? = null,
        religion: String? = null,
        gender: String? = null,
        currentUserId: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val response: Response<List<User>> = apiService.searchUsers(
                    minAge = minAge,
                    maxAge = maxAge,
                    name = name,
                    location = location,
                    religion = religion,
                    gender = gender,
                    currentUserId = currentUserId
                )
                if (response.isSuccessful) {
                    response.body()?.let { _searchResults.value = it }
                } else {
                    _error.value = "Search failed: ${response.message()}"
                }
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearResults() {
        _searchResults.value = emptyList()
    }
}
