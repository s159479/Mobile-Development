package edu.ap.tobadtoostay.ui.store

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.local.AppDatabase
import edu.ap.tobadtoostay.data.model.MapPoint
import edu.ap.tobadtoostay.data.model.Store
import edu.ap.tobadtoostay.data.model.StoreCategory
import edu.ap.tobadtoostay.data.remote.NominatimApi
import edu.ap.tobadtoostay.data.repository.GeocodingRepository
import edu.ap.tobadtoostay.data.repository.StoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

data class MyStoreUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isLookingUpAddress: Boolean = false,
    val isExistingStore: Boolean = false,
    val name: String = "",
    val category: StoreCategory? = null,
    val pin: MapPoint? = null,
    val street: String = "",
    val postalCode: String = "",
    val city: String = "",
    // The map only moves to this point when it changes (store loaded), not on every tap
    val mapCenter: MapPoint = DEFAULT_CENTER,
    @StringRes val nameError: Int? = null,
    @StringRes val categoryError: Int? = null,
    @StringRes val pinError: Int? = null,
    @StringRes val streetError: Int? = null,
    @StringRes val postalCodeError: Int? = null,
    @StringRes val cityError: Int? = null,
    @StringRes val message: Int? = null
) {
    companion object {
        // Antwerp, used until the merchant places a pin
        val DEFAULT_CENTER = MapPoint(51.2194, 4.4025)
    }
}

class MyStoreViewModel(application: Application) : AndroidViewModel(application) {

    private val storeRepository = StoreRepository()
    private val geocodingRepository = GeocodingRepository(
        api = NominatimApi.instance,
        cache = AppDatabase.getInstance(application).cachedAddressDao()
    )

    private val _uiState = MutableStateFlow(MyStoreUiState())
    val uiState: StateFlow<MyStoreUiState> = _uiState.asStateFlow()

    private var addressJob: Job? = null

    init {
        loadStore()
    }

    private fun loadStore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val store = storeRepository.getMyStore()
                if (store != null) {
                    val point = MapPoint(store.latitude, store.longitude)
                    _uiState.update {
                        it.copy(
                            isExistingStore = true,
                            name = store.name,
                            category = StoreCategory.fromKey(store.category),
                            pin = point,
                            mapCenter = point,
                            street = store.street,
                            postalCode = store.postalCode,
                            city = store.city
                        )
                    }
                }
            } catch (e: FirebaseFirestoreException) {
                _uiState.update { it.copy(message = errorMessage(e)) }
            } catch (e: IllegalStateException) {
                // Not logged in
                _uiState.update { it.copy(message = R.string.store_error_generic) }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onNameChange(value: String) =
        _uiState.update { it.copy(name = value, nameError = null) }

    fun onCategoryChange(value: StoreCategory) =
        _uiState.update { it.copy(category = value, categoryError = null) }

    fun onStreetChange(value: String) =
        _uiState.update { it.copy(street = value, streetError = null) }

    fun onPostalCodeChange(value: String) =
        _uiState.update { it.copy(postalCode = value, postalCodeError = null) }

    fun onCityChange(value: String) =
        _uiState.update { it.copy(city = value, cityError = null) }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    /** H1 + H2: the merchant taps the map, we place the pin and fill in the address. */
    fun onMapTapped(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(pin = MapPoint(latitude, longitude), pinError = null) }

        // A new tap cancels the lookup of the previous pin
        addressJob?.cancel()
        addressJob = viewModelScope.launch {
            _uiState.update { it.copy(isLookingUpAddress = true) }
            try {
                val address = geocodingRepository.reverse(latitude, longitude)
                val street = listOf(address.road, address.houseNumber)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                _uiState.update {
                    it.copy(
                        street = street,
                        postalCode = address.postcode,
                        city = address.city,
                        streetError = null,
                        postalCodeError = null,
                        cityError = null,
                        message = if (street.isBlank()) R.string.store_address_not_found else null
                    )
                }
            } catch (e: IOException) {
                _uiState.update { it.copy(message = R.string.store_address_lookup_failed) }
            } catch (e: retrofit2.HttpException) {
                _uiState.update { it.copy(message = R.string.store_address_lookup_failed) }
            }
            _uiState.update { it.copy(isLookingUpAddress = false) }
        }
    }

    fun onSaveClick() {
        if (!validate()) return
        val state = _uiState.value
        val pin = state.pin ?: return

        val store = Store(
            name = state.name.trim(),
            category = state.category!!.key,
            latitude = pin.latitude,
            longitude = pin.longitude,
            street = state.street.trim(),
            postalCode = state.postalCode.trim(),
            city = state.city.trim()
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                // Offline, Firestore keeps the write and sends it later, but await() would wait forever
                val saved = withTimeoutOrNull(SAVE_TIMEOUT_MS) { storeRepository.saveMyStore(store) }
                _uiState.update {
                    it.copy(
                        isExistingStore = true,
                        message = if (saved != null) R.string.store_saved else R.string.store_saved_offline
                    )
                }
            } catch (e: FirebaseFirestoreException) {
                _uiState.update { it.copy(message = errorMessage(e)) }
            } catch (e: IllegalStateException) {
                _uiState.update { it.copy(message = R.string.store_error_generic) }
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun validate(): Boolean {
        val state = _uiState.value
        val nameError = if (state.name.isBlank()) R.string.store_error_name else null
        val categoryError = if (state.category == null) R.string.store_error_category else null
        val pinError = if (state.pin == null) R.string.store_error_pin else null
        val streetError = if (state.street.isBlank()) R.string.store_error_street else null
        val postalCodeError = if (!state.postalCode.trim().matches(Regex("\\d{4}"))) {
            R.string.store_error_postal_code
        } else {
            null
        }
        val cityError = if (state.city.isBlank()) R.string.store_error_city else null

        _uiState.update {
            it.copy(
                nameError = nameError,
                categoryError = categoryError,
                pinError = pinError,
                streetError = streetError,
                postalCodeError = postalCodeError,
                cityError = cityError
            )
        }
        return listOf(nameError, categoryError, pinError, streetError, postalCodeError, cityError)
            .all { it == null }
    }

    @StringRes
    private fun errorMessage(e: FirebaseFirestoreException): Int = when (e.code) {
        FirebaseFirestoreException.Code.UNAVAILABLE -> R.string.error_no_internet
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> R.string.store_error_permission
        else -> R.string.store_error_generic
    }

    companion object {
        private const val SAVE_TIMEOUT_MS = 10_000L
    }
}
