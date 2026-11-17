package edu.ap.tobadtoostay.ui.nearby

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
import androidx.compose.ui.unit.dp
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.model.NearbyBag
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** List of the bags near the customer, the closest first (K3). */
@Composable
fun NearbyListScreen(
    state: NearbyBagsUiState,
    onRetry: () -> Unit,
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
                        BagCard(nearbyBag)
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

// TODO (when Jasper adds the fields to Bag): small photo and the original price (struck through)
@Composable
private fun BagCard(nearbyBag: NearbyBag) {
    val bag = nearbyBag.bag

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = bag.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = nearbyBag.store.name,
                style = MaterialTheme.typography.bodyMedium
            )

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
                Text(
                    text = stringResource(R.string.bag_price, bag.price),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(R.string.bag_left, bag.quantityLeft))
                Text(stringResource(R.string.bag_distance, nearbyBag.distanceMeters / 1000))
            }
        }
    }
}

/** 17:30 */
private fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
