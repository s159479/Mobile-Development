package edu.ap.tobadtoostay.ui.nearby

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.model.NearbyBag
import edu.ap.tobadtoostay.data.model.Store
import edu.ap.tobadtoostay.ui.FavoriteButton
import edu.ap.tobadtoostay.ui.formatTime

/** List of the bags near the customer, the closest first (K3). */
@Composable
fun NearbyListScreen(
    state: NearbyBagsUiState,
    onRetry: () -> Unit,
    onBagClick: (String) -> Unit,
    favoriteIds: Set<String>,
    onFavoriteChange: (Store, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        NearbyBagsUiState.WaitingForLocation, NearbyBagsUiState.Loading -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.nearby_loading),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        is NearbyBagsUiState.Loaded -> {
            if (state.bags.isEmpty()) {
                val km = (state.radiusMeters / 1000).toInt()
                Text(
                    text = stringResource(R.string.nearby_empty, km),
                    modifier = modifier.padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.bags, key = { it.bag.id }) { nearbyBag ->
                        BagCard(
                            nearbyBag = nearbyBag,
                            isFavorite = nearbyBag.store.id in favoriteIds,
                            onClick = { onBagClick(nearbyBag.bag.id) },
                            onFavoriteChange = { favorite -> onFavoriteChange(nearbyBag.store, favorite) }
                        )
                    }
                }
            }
        }

        is NearbyBagsUiState.Error -> {
            Column(modifier = modifier.padding(16.dp)) {
                Text(
                    text = stringResource(state.message),
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(stringResource(R.string.location_retry))
                }
            }
        }
    }
}

// TODO small photo
@Composable
private fun BagCard(
    nearbyBag: NearbyBag,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onFavoriteChange: (Boolean) -> Unit
) {
    val bag = nearbyBag.bag

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bag.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = nearbyBag.store.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                FavoriteButton(
                    isFavorite = isFavorite,
                    storeName = nearbyBag.store.name,
                    onFavoriteChange = onFavoriteChange
                )
            }

            if (bag.pickupStart != null && bag.pickupEnd != null) {
                Text(
                    text = stringResource(
                        R.string.bag_pickup,
                        formatTime(bag.pickupStart),
                        formatTime(bag.pickupEnd)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.bag_price, bag.price),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (bag.originalValue > bag.price) {
                        Text(
                            text = stringResource(R.string.bag_original_value, bag.originalValue),
                            style = MaterialTheme.typography.bodySmall,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
                Text(stringResource(R.string.bag_left, bag.quantityLeft))
                Text(stringResource(R.string.bag_distance, nearbyBag.distanceMeters / 1000))
            }
        }
    }
}
