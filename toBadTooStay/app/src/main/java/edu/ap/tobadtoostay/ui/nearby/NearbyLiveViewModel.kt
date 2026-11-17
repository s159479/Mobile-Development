package edu.ap.tobadtoostay.ui.nearby

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestoreException
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.location.GeoMath
import edu.ap.tobadtoostay.data.model.MapPoint
import edu.ap.tobadtoostay.data.repository.BagRepository
import edu.ap.tobadtoostay.data.repository.NearbyBagsRepository
import edu.ap.tobadtoostay.data.repository.StoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Bags near the customer, kept up to date in real time (K5).
 * Used by the list (K3) and the map (K4). It uses the same [NearbyBagsUiState] as
 * [NearbyBagsViewModel], but listens to Firestore instead of loading once.
 */
class NearbyLiveViewModel(application: Application) : AndroidViewModel(application) {

    private val storeRepository = StoreRepository()
    private val bagRepository = BagRepository()

    private val _uiState = MutableStateFlow<NearbyBagsUiState>(NearbyBagsUiState.WaitingForLocation)
    val uiState: StateFlow<NearbyBagsUiState> = _uiState.asStateFlow()

    private var job: Job? = null
    private var lastCenter: MapPoint? = null

    /** Starts listening for bags around [center]. Skipped when we already listen for the same place. */
    fun load(center: MapPoint) {
        val previous = lastCenter
        val alreadyBusy = _uiState.value is NearbyBagsUiState.Loaded || _uiState.value is NearbyBagsUiState.Loading
        if (alreadyBusy && previous != null && GeoMath.distanceMeters(previous, center) < SAME_PLACE_METERS) return

        lastCenter = center
        job?.cancel()

        // Without internet Firestore would show an empty cached list, so say it clearly instead
        if (!isOnline()) {
            _uiState.value = NearbyBagsUiState.Error(R.string.error_no_internet)
            return
        }

        job = viewModelScope.launch {
            _uiState.value = NearbyBagsUiState.Loading
            val radius = NearbyBagsRepository.DEFAULT_RADIUS_METERS
            try {
                val stores = storeRepository.getStoresNear(center, radius)
                // collect keeps running: every change in Firestore gives a new list
                bagRepository.observeBagsForStores(stores.map { it.id }).collect { bags ->
                    val nearbyBags = NearbyBagsRepository.combine(center, stores, bags, System.currentTimeMillis())
                    _uiState.value = NearbyBagsUiState.Loaded(nearbyBags, radius)
                }
            } catch (e: FirebaseFirestoreException) {
                _uiState.value = NearbyBagsUiState.Error(
                    if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) R.string.error_no_internet
                    else R.string.nearby_error
                )
            }
        }
    }

    /** Retry button: starts again for the last location. */
    fun retry() {
        val center = lastCenter ?: return
        lastCenter = null
        _uiState.value = NearbyBagsUiState.WaitingForLocation
        load(center)
    }

    private fun isOnline(): Boolean {
        val manager = getApplication<Application>().getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    private companion object {
        const val SAME_PLACE_METERS = 100.0
    }
}
