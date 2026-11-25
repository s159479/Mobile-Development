package edu.ap.tobadtoostay.ui.orders

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import edu.ap.tobadtoostay.data.local.AppDatabase
import edu.ap.tobadtoostay.data.model.Order
import edu.ap.tobadtoostay.data.model.Orders
import edu.ap.tobadtoostay.data.repository.OrdersRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Is the list in Room up to date with Firestore? */
enum class SyncStatus {
    UP_TO_DATE,

    /** No internet: the list is what was saved on the phone. */
    OFFLINE,

    /** Firestore refused (for example no permission): the list is what was saved on the phone. */
    FAILED
}

sealed interface OrdersUiState {
    data object Loading : OrdersUiState

    data class Loaded(
        val active: List<Order>,
        val past: List<Order>,
        val syncStatus: SyncStatus
    ) : OrdersUiState
}

/**
 * Orders of the customer (K10). The list comes only from Room. As long as this ViewModel lives,
 * every change in Firestore is copied to Room, so the list is also right without internet.
 */
class OrdersViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OrdersRepository(AppDatabase.getInstance(application).cachedReservationDao())

    // Fixed for the life of this ViewModel: logging out closes MainActivity (CLEAR_TASK),
    // so the next user always gets a new ViewModel.
    private val customerId = repository.currentCustomerId().orEmpty()

    private val syncStatus = MutableStateFlow(SyncStatus.UP_TO_DATE)

    // Every minute a new time, so an order moves to "Niet opgehaald" while the screen is open
    private val clock = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(CLOCK_INTERVAL_MS)
        }
    }

    val uiState: StateFlow<OrdersUiState> =
        combine(repository.observeCached(customerId), syncStatus, clock) { reservations, status, now ->
            val split = Orders.split(reservations, now)
            OrdersUiState.Loaded(split.active, split.past, status)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), OrdersUiState.Loading)

    init {
        if (customerId.isNotEmpty()) startSync()
    }

    private fun startSync() {
        viewModelScope.launch {
            repository.syncFromFirestore(customerId)
                // Connection problems: show the saved list and try again after a while
                .retryWhen { cause, _ ->
                    val retry = cause is FirebaseFirestoreException &&
                        cause.code == FirebaseFirestoreException.Code.UNAVAILABLE
                    if (retry) {
                        syncStatus.value = SyncStatus.OFFLINE
                        delay(RETRY_DELAY_MS)
                    }
                    retry
                }
                // Any other Firestore error: stop syncing and keep showing what Room has
                .catch { e ->
                    if (e is FirebaseFirestoreException) syncStatus.value = SyncStatus.FAILED else throw e
                }
                .collect { offline ->
                    syncStatus.value = if (offline) SyncStatus.OFFLINE else SyncStatus.UP_TO_DATE
                }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val CLOCK_INTERVAL_MS = 60_000L
        const val RETRY_DELAY_MS = 10_000L
    }
}
