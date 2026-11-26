package edu.ap.tobadtoostay.ui.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.local.entity.FavoriteStore
import edu.ap.tobadtoostay.ui.FavoriteButton
import kotlinx.coroutines.launch

/** Tab "Favorieten": the stores the customer marked with a heart (K12), read from Room. */
@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    snackbarHostState: SnackbarHostState,
    onRemove: (FavoriteStore) -> Unit,
    onUndoRemove: (FavoriteStore) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Removing is one tap, so show a snackbar with "Ongedaan maken" in case it was a mistake
    val removeWithUndo: (FavoriteStore) -> Unit = { store ->
        onRemove(store)
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.favorites_removed, store.name),
                actionLabel = context.getString(R.string.favorites_undo)
            )
            if (result == SnackbarResult.ActionPerformed) onUndoRemove(store)
        }
    }

    when (state) {
        FavoritesUiState.Loading -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.favorites_loading),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        is FavoritesUiState.Loaded -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.favorites_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                if (state.stores.isEmpty()) {
                    item { Text(stringResource(R.string.favorites_empty)) }
                }
                items(state.stores, key = { it.storeId }) { store ->
                    FavoriteCard(
                        store = store,
                        onRemove = { removeWithUndo(store) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(store: FavoriteStore, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = store.name, style = MaterialTheme.typography.titleMedium)
                Text(text = store.address, style = MaterialTheme.typography.bodyMedium)
            }
            // every card here is a favorite, so the heart is always filled
            FavoriteButton(
                isFavorite = true,
                storeName = store.name,
                onFavoriteChange = { onRemove() }
            )
        }
    }
}
