package edu.ap.tobadtoostay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
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

class HomeViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

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

    fun logout() {
        FirebaseAuth.getInstance().signOut()
    }
}
