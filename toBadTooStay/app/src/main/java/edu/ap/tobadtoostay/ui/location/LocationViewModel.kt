package edu.ap.tobadtoostay.ui.location

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.ap.tobadtoostay.data.model.MapPoint
import edu.ap.tobadtoostay.data.repository.LocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LocationUiState {
    /** The user did not allow the location (yet). */
    data object NoPermission : LocationUiState

    /** Allowed, but location is turned off on the device. */
    data object LocationOff : LocationUiState

    data object Loading : LocationUiState

    data class Found(val point: MapPoint) : LocationUiState

    /** Allowed and turned on, but no location could be found. */
    data object NotFound : LocationUiState
}

/** Holds the current location of the user, used later for "packages nearby". */
class LocationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocationRepository(application)

    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.NoPermission)
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    private var job: Job? = null

    init {
        // Start right away, so users who already allowed it don't see the "allow" button flash
        refresh()
    }

    /**
     * Called when the screen is shown or the permission changed. Does nothing when we already
     * have a location or are looking for one, so it is safe to call more than once.
     */
    fun ensureLocation() {
        val state = _uiState.value
        if (state !is LocationUiState.Found && state !is LocationUiState.Loading) refresh()
    }

    /** Looks up the location again (retry button). */
    fun refresh() {
        // A lookup that is still running may not overwrite the new state
        job?.cancel()

        if (!repository.hasPermission()) {
            _uiState.value = LocationUiState.NoPermission
            return
        }
        if (!repository.isLocationEnabled()) {
            _uiState.value = LocationUiState.LocationOff
            return
        }

        _uiState.value = LocationUiState.Loading
        job = viewModelScope.launch {
            _uiState.value = try {
                repository.getCurrentLocation()?.let { LocationUiState.Found(it) }
                    ?: LocationUiState.NotFound
            } catch (e: SecurityException) {
                // Permission was taken away while we were looking
                LocationUiState.NoPermission
            }
        }
    }
}
