package edu.ap.tobadtoostay.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.model.NearbyBag
import edu.ap.tobadtoostay.ui.BackArrow
import edu.ap.tobadtoostay.ui.map.OsmMap
import edu.ap.tobadtoostay.ui.nearby.formatTime
import org.osmdroid.util.GeoPoint

@Composable
fun BagDetailScreen(
    nearbyBag: NearbyBag?,
    isLoading: Boolean,
    reservationState: ReservationUiState,
    onReserve: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BackArrow(onClick = onBack)

        // the last bag leaves the list after reserving, but the code must stay visible
        if (reservationState is ReservationUiState.Success) {
            PickupCodeCard(reservationState)
        }

        if (nearbyBag == null) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (reservationState !is ReservationUiState.Success) {
                Text(stringResource(R.string.detail_not_available))
            }
        } else {
            BagInfo(nearbyBag)
            if (reservationState !is ReservationUiState.Success) {
                ReserveSection(nearbyBag.bag.quantityLeft, reservationState, onReserve)
            }
        }
    }
}

@Composable
private fun BagInfo(nearbyBag: NearbyBag) {
    val bag = nearbyBag.bag
    val store = nearbyBag.store
    val storePosition = GeoPoint(store.latitude, store.longitude)

    // TODO show the photo when Bag has photoBase64
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.detail_no_photo),
            style = MaterialTheme.typography.bodySmall
        )
    }

    Text(text = bag.title, style = MaterialTheme.typography.headlineSmall)
    Text(text = store.name, style = MaterialTheme.typography.titleMedium)
    Text(text = store.address, style = MaterialTheme.typography.bodyMedium)

    if (bag.description.isNotBlank()) {
        Text(text = bag.description, modifier = Modifier.padding(top = 8.dp))
    }

    if (bag.pickupStart != null && bag.pickupEnd != null) {
        Text(
            text = stringResource(
                R.string.bag_pickup,
                formatTime(bag.pickupStart),
                formatTime(bag.pickupEnd)
            )
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.bag_price, bag.price),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (bag.originalValue > bag.price) {
                Text(
                    text = stringResource(R.string.bag_original_value, bag.originalValue),
                    textDecoration = TextDecoration.LineThrough
                )
            }
        }
        Text(stringResource(R.string.bag_left, bag.quantityLeft))
        Text(stringResource(R.string.bag_distance, nearbyBag.distanceMeters / 1000))
    }

    OsmMap(
        center = storePosition,
        zoom = 15.0,
        marker = storePosition,
        markerTitle = store.name,
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clipToBounds() // otherwise the map draws over the rest of the screen
    )
}

@Composable
private fun ReserveSection(
    quantityLeft: Int,
    reservationState: ReservationUiState,
    onReserve: (Int) -> Unit
) {
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    // someone else may have taken bags in the meantime
    val shownQuantity = quantity.coerceIn(1, quantityLeft.coerceAtLeast(1))
    val isLoading = reservationState is ReservationUiState.Loading

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
    ) {
        OutlinedButton(
            onClick = { quantity = shownQuantity - 1 },
            enabled = shownQuantity > 1 && !isLoading
        ) {
            Text(stringResource(R.string.detail_minus))
        }
        Text(text = "$shownQuantity", style = MaterialTheme.typography.titleLarge)
        OutlinedButton(
            onClick = { quantity = shownQuantity + 1 },
            enabled = shownQuantity < quantityLeft && !isLoading
        ) {
            Text(stringResource(R.string.detail_plus))
        }
    }

    if (reservationState is ReservationUiState.Error) {
        Text(
            text = stringResource(reservationState.message),
            color = MaterialTheme.colorScheme.error
        )
    }

    Button(
        onClick = { onReserve(shownQuantity) },
        enabled = !isLoading,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
            Text(stringResource(R.string.detail_reserve))
        }
    }
}

@Composable
private fun PickupCodeCard(state: ReservationUiState.Success) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.reserve_success, state.quantity),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = state.pickupCode,
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(stringResource(R.string.reserve_show_code))
        }
    }
}
