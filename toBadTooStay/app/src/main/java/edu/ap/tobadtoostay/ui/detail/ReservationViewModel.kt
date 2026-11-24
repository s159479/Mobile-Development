package edu.ap.tobadtoostay.ui.detail

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.model.NearbyBag
import edu.ap.tobadtoostay.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReservationUiState {
    data object Idle : ReservationUiState

    data object Loading : ReservationUiState

    data class Success(val pickupCode: String, val quantity: Int) : ReservationUiState

    data class Error(@StringRes val message: Int) : ReservationUiState
}

class ReservationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReservationRepository()

    private val _uiState = MutableStateFlow<ReservationUiState>(ReservationUiState.Idle)
    val uiState: StateFlow<ReservationUiState> = _uiState.asStateFlow()

    fun reserve(nearbyBag: NearbyBag, quantity: Int) {
        if (_uiState.value is ReservationUiState.Loading) return

        if (!isOnline()) {
            _uiState.value = ReservationUiState.Error(R.string.error_no_internet)
            return
        }

        viewModelScope.launch {
            _uiState.value = ReservationUiState.Loading
            try {
                val code = repository.reserve(nearbyBag, quantity)
                _uiState.value = ReservationUiState.Success(code, quantity)
            } catch (e: FirebaseFirestoreException) {
                _uiState.value = ReservationUiState.Error(
                    when (e.code) {
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION -> R.string.reserve_error_sold_out
                        FirebaseFirestoreException.Code.OUT_OF_RANGE -> R.string.reserve_error_pickup_over
                        FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_no_internet
                        else -> R.string.reserve_error_failed
                    }
                )
            } catch (e: IllegalStateException) {
                _uiState.value = ReservationUiState.Error(R.string.reserve_error_failed)
            }
        }
    }

    private fun isOnline(): Boolean {
        val manager = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
