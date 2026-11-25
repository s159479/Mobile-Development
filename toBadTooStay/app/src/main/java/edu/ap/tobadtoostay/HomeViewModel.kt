package edu.ap.tobadtoostay

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import edu.ap.tobadtoostay.data.local.AppDatabase
import edu.ap.tobadtoostay.data.repository.OrdersRepository
import edu.ap.tobadtoostay.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** A2: which start screen a logged in user gets. */
sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** The role could not be loaded (no internet, or no profile). */
    data object Failed : HomeUiState

    data object Customer : HomeUiState

    data class Merchant(val email: String) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository()
    private val ordersRepository = OrdersRepository(AppDatabase.getInstance(application).cachedReservationDao())

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadRole()
    }

    /** Looks up the role of the logged in user (also used by the retry button). */
    fun loadRole() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            _uiState.value = try {
                val role = userRepository.getMyRole()
                if (role == null) {
                    HomeUiState.Failed
                } else if (role == UserRepository.ROLE_CUSTOMER) {
                    HomeUiState.Customer
                } else {
                    HomeUiState.Merchant(FirebaseAuth.getInstance().currentUser?.email ?: "")
                }
            } catch (e: FirebaseFirestoreException) {
                HomeUiState.Failed
            }
        }
    }

    private var loggingOut = false

    /**
     * Logs out, removes the saved orders (pickup codes) from the phone, then calls [onDone].
     * This lives here because logout is the one moment the app leaves the account behind.
     * First sign out, so the orders listener can't write to Room again after the cache is empty.
     */
    fun logout(onDone: () -> Unit) {
        // a second tap while logging out does nothing
        if (loggingOut) return
        loggingOut = true

        viewModelScope.launch {
            FirebaseAuth.getInstance().signOut()
            ordersRepository.clearCache()
            onDone()
        }
    }
}
