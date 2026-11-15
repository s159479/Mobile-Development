package edu.ap.tobadtoostay.ui.location

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.location.LocationAccess

/**
 * Asks the location permission and shows if we know the location of the user.
 * The permission is asked by a button, so the user first reads why we need it.
 */
@Composable
fun LocationCard(
    modifier: Modifier = Modifier,
    viewModel: LocationViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permission = rememberLocationPermissionState()

    // Runs when the screen comes back (the location setting may have changed outside the app)
    // and when the permission changes. ensureLocation() skips it if we already have a location.
    LifecycleResumeEffect(permission.access) {
        viewModel.ensureLocation()
        onPauseOrDispose { }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.location_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            when (state) {
                LocationUiState.NoPermission -> {
                    Text(stringResource(R.string.location_rationale))
                    if (permission.isPermanentlyDenied) {
                        Text(
                            text = stringResource(R.string.location_permanently_denied),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    CardButton(
                        text = stringResource(
                            if (permission.isPermanentlyDenied) R.string.location_open_settings
                            else R.string.location_allow
                        ),
                        onClick = permission::request
                    )
                }

                LocationUiState.LocationOff -> {
                    Text(stringResource(R.string.location_off))
                    CardButton(
                        text = stringResource(R.string.location_turn_on),
                        onClick = { context.startActivitySafely(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) }
                    )
                }

                LocationUiState.Loading -> {
                    Text(stringResource(R.string.location_loading))
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                is LocationUiState.Found -> {
                    Text(stringResource(R.string.location_found))
                    if (permission.access == LocationAccess.APPROXIMATE) {
                        Text(
                            text = stringResource(R.string.location_approximate),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                LocationUiState.NotFound -> {
                    Text(
                        text = stringResource(R.string.location_not_found),
                        color = MaterialTheme.colorScheme.error
                    )
                    CardButton(text = stringResource(R.string.location_retry), onClick = viewModel::refresh)
                }
            }
        }
    }
}

@Composable
private fun CardButton(text: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Text(text)
    }
}
