// file: viewmodel/AccountViewModel.kt

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mnvths.schoolclearance.data.Account
import com.mnvths.schoolclearance.network.KtorClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import timber.log.Timber // --- LOGGING ADDED ---

// ✅ ADDED: A small helper class for the request body
@Serializable
private data class AccountRequest(val accountName: String)

class AccountViewModel : ViewModel() {
    private val client = KtorClient.httpClient

    private val _accounts = mutableStateOf<List<Account>>(emptyList())
    val accounts: State<List<Account>> = _accounts

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    init {
        // --- LOGGING ADDED ---
        Timber.i("AccountViewModel initialized.")
        fetchAccounts()
    }

    fun fetchAccounts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // --- LOGGING ADDED ---
            Timber.i("Fetching accounts.")
            try {
                val response: List<Account> = client.get("/accounts").body()
                _accounts.value = response.sortedBy { it.name }
                // --- LOGGING ADDED ---
                Timber.i("Successfully fetched ${_accounts.value.size} accounts.")
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                // --- LOGGING ADDED ---
                Timber.e(e, "Error fetching accounts.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ✅ ADDED: Function to add a new account
    fun addAccount(name: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to add account: '%s'", name)
            try {
                val response: HttpResponse = client.post("/accounts") {
                    contentType(ContentType.Application.Json)
                    setBody(AccountRequest(accountName = name))
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully added account: '%s'", name)
                    onSuccess()
                    fetchAccounts() // Refresh the list
                } else {
                    val errorBody = response.body<Map<String, String>>()
                    val errorMessage = errorBody["error"] ?: "Failed to add account."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to add account. Server error: %s", errorMessage)
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during add account network call.")
                onError("Network Error: ${e.message}")
            }
        }
    }

    // ✅ ADDED: Function to update an existing account
    fun updateAccount(id: Int, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to update account ID: %d to new name: '%s'", id, newName)
            try {
                val response: HttpResponse = client.put("/accounts/$id") {
                    contentType(ContentType.Application.Json)
                    setBody(AccountRequest(accountName = newName))
                }
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully updated account ID: %d", id)
                    onSuccess()
                    fetchAccounts() // Refresh the list
                } else {
                    val errorBody = response.body<Map<String, String>>()
                    val errorMessage = errorBody["error"] ?: "Failed to update account."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to update account ID %d. Server error: %s", id, errorMessage)
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during update account network call.")
                onError("Network Error: ${e.message}")
            }
        }
    }

    // ✅ ADDED: Function to delete an account
    fun deleteAccount(id: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            // --- LOGGING ADDED ---
            Timber.i("Attempting to delete account ID: %d", id)
            try {
                val response: HttpResponse = client.delete("/accounts/$id")
                if (response.status.isSuccess()) {
                    // --- LOGGING ADDED ---
                    Timber.i("Successfully deleted account ID: %d", id)
                    onSuccess()
                    fetchAccounts() // Refresh the list
                } else {
                    val errorBody = response.body<Map<String, String>>()
                    val errorMessage = errorBody["error"] ?: "Failed to delete account."
                    // --- LOGGING ADDED ---
                    Timber.w("Failed to delete account ID %d. Server error: %s", id, errorMessage)
                    onError(errorMessage)
                }
            } catch (e: Exception) {
                // --- LOGGING ADDED ---
                Timber.e(e, "Error during delete account network call.")
                onError("Network Error: ${e.message}")
            }
        }
    }
}