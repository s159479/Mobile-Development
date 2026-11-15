package edu.ap.tobadtoostay.ui.nearby

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.location.GeoMath
import edu.ap.tobadtoostay.data.model.MapPoint
import edu.ap.tobadtoostay.data.model.NearbyBag
import edu.ap.tobadtoostay.data.repository.NearbyBagsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface NearbyBagsUiState {
    /** Waiting for the location of the customer. */
    data object WaitingForLocation : NearbyBagsUiState

    data object Loading : NearbyBagsUiState

    data class Loaded(val bags: List<NearbyBag>, val radiusMeters: Double) : NearbyBagsUiState

    data class Error(@StringRes val message: Int) : NearbyBagsUiState
}

/** Bags near the customer. Shared by the list and the map of the customer later on. */
class NearbyBagsViewModel(
    private val repository: NearbyBagsRepository = NearbyBagsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<NearbyBagsUiState>(NearbyBagsUiState.WaitingForLocation)
    val uiState: StateFlow<NearbyBagsUiState> = _uiState.asStateFlow()

    private var job: Job? = null
    private var lastCenter: MapPoint? = null

    /** Loads the bags around [center]. Skipped when we already have them for (almost) the same place. */
    fun load(center: MapPoint) {
        val previous = lastCenter
        val alreadyLoaded = _uiState.value is NearbyBagsUiState.Loaded || _uiState.value is NearbyBagsUiState.Loading
        if (alreadyLoaded && previous != null && GeoMath.distanceMeters(previous, center) < SAME_PLACE_METERS) return

        lastCenter = center
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.value = NearbyBagsUiState.Loading
            _uiState.value = try {
                NearbyBagsUiState.Loaded(
                    bags = repository.getNearbyBags(center),
                    radiusMeters = NearbyBagsRepository.DEFAULT_RADIUS_METERS
                )
            } catch (e: FirebaseFirestoreException) {
                NearbyBagsUiState.Error(
                    if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) R.string.error_no_internet
                    else R.string.nearby_error
                )
            }
        }
    }

    /** Retry button: loads again for the last location. */
    fun retry() {
        val center = lastCenter ?: return
        _uiState.value = NearbyBagsUiState.WaitingForLocation
        load(center)
    }

    private companion object {
        /** Moving less than this does not change which stores are nearby in a noticeable way. */
        const val SAME_PLACE_METERS = 100.0
    }
}
