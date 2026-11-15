package edu.ap.tobadtoostay.ui.nearby

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.model.MapPoint
import java.util.Locale

/**
 * Short summary of the bags near the customer. The full list and the map are separate tasks
 * ("lijstweergave" and "kaartweergave") and can use the same [NearbyBagsViewModel].
 * Shows nothing until the location of the customer is known.
 */
@Composable
fun NearbyBagsSummary(
    location: MapPoint?,
    modifier: Modifier = Modifier,
    viewModel: NearbyBagsViewModel = viewModel()
) {
    if (location == null) return
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(location) { viewModel.load(location) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.nearby_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            when (val current = state) {
                NearbyBagsUiState.WaitingForLocation, NearbyBagsUiState.Loading -> {
                    Text(stringResource(R.string.nearby_loading))
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is NearbyBagsUiState.Loaded -> {
                    val km = (current.radiusMeters / 1000).toInt()
                    if (current.bags.isEmpty()) {
                        Text(stringResource(R.string.nearby_empty, km))
                    } else {
                        Text(pluralStringResource(R.plurals.nearby_count, current.bags.size, current.bags.size, km))
                        val closest = current.bags.first()
                        Text(
                            text = stringResource(
                                R.string.nearby_closest,
                                closest.store.name,
                                formatDistance(closest.distanceMeters)
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                is NearbyBagsUiState.Error -> {
                    Text(
                        text = stringResource(current.message),
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(
                        onClick = viewModel::retry,
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
}

/** "350 m" or "1,2 km". */
fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m"
    else String.format(Locale.forLanguageTag("nl-BE"), "%.1f km", meters / 1000)
