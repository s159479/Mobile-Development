package edu.ap.tobadtoostay.ui.nearby

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.model.MapPoint
import edu.ap.tobadtoostay.ui.map.MapMarker
import edu.ap.tobadtoostay.ui.map.OsmMap
import org.osmdroid.util.GeoPoint

/** Map with the location of the customer and one marker per store with available bags (K4). */
@Composable
fun NearbyMapScreen(
    state: NearbyBagsUiState,
    location: MapPoint,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val myPosition = GeoPoint(location.latitude, location.longitude)

    when (state) {
        NearbyBagsUiState.WaitingForLocation, NearbyBagsUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
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

        is NearbyBagsUiState.Loaded -> {
            // One marker per store, even when a store has more than one bag
            val bagsPerStore = state.bags.groupBy { it.store.id }
            val markers = bagsPerStore.values.map { bagsOfStore ->
                val store = bagsOfStore.first().store
                MapMarker(store.id, GeoPoint(store.latitude, store.longitude), store.name)
            }

            var selectedStoreId by remember { mutableStateOf<String?>(null) }
            val selectedBags = bagsPerStore[selectedStoreId]

            Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OsmMap(
                    center = myPosition,
                    zoom = 14.0,
                    marker = myPosition,
                    markerTitle = stringResource(R.string.map_your_location),
                    markers = markers,
                    onMarkerClick = { storeId -> selectedStoreId = storeId },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds() // the map view would draw over the rest of the screen
                )

                // Name of the tapped store and the "Bekijk" button
                if (selectedBags != null) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = selectedBags.first().store.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                pluralStringResource(
                                    R.plurals.map_bags_in_store,
                                    selectedBags.size,
                                    selectedBags.size
                                )
                            )
                            // The detail screen (K7) is a later task, so this button does nothing yet
                            Button(
                                onClick = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(stringResource(R.string.map_view_bag))
                            }
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.map_tap_marker),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
