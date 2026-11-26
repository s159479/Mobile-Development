package edu.ap.tobadtoostay.ui.favorites

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import edu.ap.tobadtoostay.data.local.AppDatabase
import edu.ap.tobadtoostay.data.local.entity.FavoriteStore
import edu.ap.tobadtoostay.data.model.Store
import edu.ap.tobadtoostay.data.repository.FavoritesRepository
import edu.ap.tobadtoostay.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState

    data class Loaded(val stores: List<FavoriteStore>) : FavoritesUiState
}

/**
 * Favorite stores (K12). Used by the favorites tab and by the hearts on the list
 * and the detail screen, so a heart changes everywhere at the same time.
 */
class FavoritesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FavoritesRepository(AppDatabase.getInstance(application).favoriteStoreDao())

    // Fixed for the life of this ViewModel: logging out closes MainActivity (CLEAR_TASK),
    // so the next user always gets a new ViewModel.
    private val customerId = UserRepository().currentUserId().orEmpty()

    val uiState: StateFlow<FavoritesUiState> = repository.observeAll(customerId)
        .map { FavoritesUiState.Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FavoritesUiState.Loading)

    /** The ids of the favorite stores, to fill the hearts. Built from [uiState], so Room is read once. */
    val favoriteIds: StateFlow<Set<String>> = uiState
        .map { state -> (state as? FavoritesUiState.Loaded)?.stores?.map { it.storeId }?.toSet().orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptySet())

    /**
     * [favorite] is the new state the heart asks for. A fast double tap then sets the same
     * state twice, instead of two toggles that each read an old value.
     */
    fun setFavorite(store: Store, favorite: Boolean) {
        if (customerId.isEmpty()) return
        viewModelScope.launch { repository.setFavorite(customerId, store, favorite) }
    }

    fun remove(favorite: FavoriteStore) {
        viewModelScope.launch { repository.remove(favorite) }
    }

    /** Undo of [remove]. */
    fun restore(favorite: FavoriteStore) {
        viewModelScope.launch { repository.restore(favorite) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
