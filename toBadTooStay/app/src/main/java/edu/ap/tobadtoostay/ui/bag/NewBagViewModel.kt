package edu.ap.tobadtoostay.ui.bag

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.model.Bag
import edu.ap.tobadtoostay.data.model.Store
import edu.ap.tobadtoostay.data.repository.BagRepository
import edu.ap.tobadtoostay.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class NewBagUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val hasStore: Boolean = false,
    val loadFailed: Boolean = false,
    val title: String = "",
    val description: String = "",
    val price: String = "",
    val originalValue: String = "",
    val quantity: String = "1",
    val pickupFrom: String = "17:00",
    val pickupUntil: String = "19:00",
    @StringRes val titleError: Int? = null,
    @StringRes val priceError: Int? = null,
    @StringRes val originalValueError: Int? = null,
    @StringRes val quantityError: Int? = null,
    @StringRes val pickupFromError: Int? = null,
    @StringRes val pickupUntilError: Int? = null,
    @StringRes val message: Int? = null
)

class NewBagViewModel(application: Application) : AndroidViewModel(application) {

    private val storeRepository = StoreRepository()
    private val bagRepository = BagRepository()

    private val _uiState = MutableStateFlow(NewBagUiState())
    val uiState: StateFlow<NewBagUiState> = _uiState.asStateFlow()

    private var store: Store? = null

    init {
        loadStore()
    }

    fun loadStore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, loadFailed = false) }
            try {
                store = storeRepository.getMyStore()
                _uiState.update { it.copy(isLoading = false, hasStore = store != null) }
            } catch (e: FirebaseFirestoreException) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(isLoading = false, loadFailed = true) }
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, titleError = null) }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onPriceChange(value: String) = _uiState.update { it.copy(price = value, priceError = null) }

    fun onOriginalValueChange(value: String) =
        _uiState.update { it.copy(originalValue = value, originalValueError = null) }

    fun onQuantityChange(value: String) = _uiState.update { it.copy(quantity = value, quantityError = null) }

    fun onPickupFromChange(value: String) =
        _uiState.update { it.copy(pickupFrom = value, pickupFromError = null, pickupUntilError = null) }

    fun onPickupUntilChange(value: String) =
        _uiState.update { it.copy(pickupUntil = value, pickupUntilError = null) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onSaveClick() {
        val store = store ?: return
        if (_uiState.value.isSaving || !validate()) return
        val state = _uiState.value

        if (!isOnline()) {
            _uiState.update { it.copy(message = R.string.error_no_internet) }
            return
        }

        val bag = Bag(
            title = state.title.trim(),
            description = state.description.trim(),
            price = toNumber(state.price) ?: 0.0,
            originalValue = toNumber(state.originalValue) ?: 0.0,
            quantityTotal = state.quantity.trim().toIntOrNull() ?: 0,
            pickupStart = todayAt(state.pickupFrom),
            pickupEnd = todayAt(state.pickupUntil)
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                bagRepository.createBag(bag, store)
                _uiState.update { it.copy(isSaved = true) }
            } catch (e: FirebaseFirestoreException) {
                _uiState.update { it.copy(message = errorMessage(e)) }
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun validate(): Boolean {
        val state = _uiState.value
        val price = toNumber(state.price)
        val originalValue = toNumber(state.originalValue)
        val quantity = state.quantity.trim().toIntOrNull()
        val from = todayAt(state.pickupFrom)
        val until = todayAt(state.pickupUntil)

        val titleError = if (state.title.isBlank()) R.string.bag_error_title else null
        val priceError = if (price == null || price <= 0) R.string.bag_error_price else null
        val originalValueError = if (originalValue == null || originalValue < (price ?: 0.0)) {
            R.string.bag_error_original_value
        } else {
            null
        }
        val quantityError = if (quantity == null || quantity < 1) R.string.bag_error_quantity else null
        val pickupFromError = if (from == null) R.string.bag_error_time else null
        val pickupUntilError = when {
            until == null -> R.string.bag_error_time
            from != null && until <= from -> R.string.bag_error_until_before_from
            until <= System.currentTimeMillis() -> R.string.bag_error_until_past
            else -> null
        }

        _uiState.update {
            it.copy(
                titleError = titleError,
                priceError = priceError,
                originalValueError = originalValueError,
                quantityError = quantityError,
                pickupFromError = pickupFromError,
                pickupUntilError = pickupUntilError
            )
        }
        return listOf(
            titleError, priceError, originalValueError, quantityError, pickupFromError, pickupUntilError
        ).all { it == null }
    }

    private fun toNumber(text: String): Double? = text.trim().replace(',', '.').toDoubleOrNull()

    private fun todayAt(text: String): Long? {
        val parts = text.trim().split(":")
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    @StringRes
    private fun errorMessage(e: FirebaseFirestoreException): Int = when (e.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_no_internet
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.store_error_permission
        else -> R.string.bag_error_failed
    }

    private fun isOnline(): Boolean {
        val manager = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
