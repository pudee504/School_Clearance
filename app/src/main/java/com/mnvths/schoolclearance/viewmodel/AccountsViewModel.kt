package com.mnvths.schoolclearance.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.Account
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.launch

class AccountViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _accounts = mutableStateOf<List<Account>>(emptyList())
    val accounts: State<List<Account>> = _accounts

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        fetchAccounts()
    }

    fun fetchAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                // This API endpoint needs to be created on your server
                val response: List<Account> = client.get("http://10.0.2.2:3000/accounts").body()
                _accounts.value = response.sortedBy { it.name }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    // You can add add/update/delete functions here later
}