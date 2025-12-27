package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.PaginatedUserResponse
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Response

data class MatchesUiState(
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasNextPage: Boolean = true,
    val currentPage: Int = 0,
    val pageSize: Int = 10,
    val prefetchDistance: Int = 5  // Start loading when 5 items from end
)

class MatchesViewModel : ViewModel() {
    private val apiService = RetrofitClient.apiService

    private val _uiState = MutableStateFlow(MatchesUiState())
    val uiState: StateFlow<MatchesUiState> = _uiState.asStateFlow()

    private var currentUserId: Int? = null

    fun setCurrentUserId(userId: Int) {
        currentUserId = userId
    }

    fun loadInitialUsers() {
        if (_uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val response = apiService.getAllUsers(
                    gender = null, // You can add gender filtering later
                    currentUserId = currentUserId,
                    page = 0,
                    size = _uiState.value.pageSize
                )

                handleResponse(response, isInitialLoad = true)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load users: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun loadMoreUsers() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasNextPage) return

        _uiState.update { it.copy(isLoadingMore = true, error = null) }

        viewModelScope.launch {
            try {
                val nextPage = _uiState.value.currentPage + 1
                val response = apiService.getAllUsers(
                    gender = null,
                    currentUserId = currentUserId,
                    page = nextPage,
                    size = _uiState.value.pageSize
                )

                handleResponse(response, isInitialLoad = false)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoadingMore = false,
                        error = "Failed to load more users: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun handleResponse(response: Response<PaginatedUserResponse>, isInitialLoad: Boolean) {
        if (response.isSuccessful) {
            response.body()?.let { paginatedResponse ->
                _uiState.update { currentState ->
                    val newUsers = if (isInitialLoad) {
                        paginatedResponse.content
                    } else {
                        currentState.users + paginatedResponse.content
                    }

                    currentState.copy(
                        users = newUsers,
                        isLoading = false,
                        isLoadingMore = false,
                        error = null,
                        hasNextPage = paginatedResponse.hasNext,
                        currentPage = if (isInitialLoad) 0 else currentState.currentPage + 1
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = "Failed to load users: ${response.message()}"
                )
            }
        }
    }

    fun shouldLoadMore(visibleItemCount: Int, lastVisibleItemPosition: Int): Boolean {
        val state = _uiState.value
        if (state.isLoadingMore || !state.hasNextPage) return false

        // Load more when we're within prefetch distance of the end
        val totalItems = state.users.size
        return lastVisibleItemPosition >= (totalItems - state.prefetchDistance)
    }

    fun refresh() {
        _uiState.update { MatchesUiState() }
        loadInitialUsers()
    }

    fun retry() {
        if (_uiState.value.users.isEmpty()) {
            loadInitialUsers()
        } else {
            loadMoreUsers()
        }
    }
}
