package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.ErrorResponse
import com.example.myapplication.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class SearchViewModel : ViewModel() {

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun searchUsers(
        minAge: Int? = null,
        maxAge: Int? = null,
        name: String? = null,
        location: String? = null,
        religion: String? = null,
        gender: String? = null
    ) {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val response: Response<List<User>> = RetrofitClient.apiService.searchUsers(
                    minAge, maxAge, name, location, religion, gender
                )

                if (response.isSuccessful) {
                    response.body()?.let { _searchResults.value = it }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        try {
                            val errorResponse = RetrofitClient.gson.fromJson(errorBody, ErrorResponse::class.java)
                            errorResponse.error
                        } catch (e: Exception) {
                            "Search failed: ${response.message()}"
                        }
                    } else {
                        "Search failed: ${response.message()}"
                    }
                    _error.value = errorMessage
                }
            } catch (e: Exception) {
                _error.value = "Search failed: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
