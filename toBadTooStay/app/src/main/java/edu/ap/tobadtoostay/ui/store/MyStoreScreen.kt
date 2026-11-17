package edu.ap.tobadtoostay.ui.store

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.model.StoreCategory
import edu.ap.tobadtoostay.ui.map.OsmMap
import org.osmdroid.util.GeoPoint

@Composable
fun MyStoreScreen(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: MyStoreViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageText = state.message?.let { stringResource(it) }

    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.onMessageShown()
        }
    }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    MyStoreContent(
        state = state,
        onNameChange = viewModel::onNameChange,
        onCategoryChange = viewModel::onCategoryChange,
        onMapTapped = { viewModel.onMapTapped(it.latitude, it.longitude) },
        onStreetChange = viewModel::onStreetChange,
        onPostalCodeChange = viewModel::onPostalCodeChange,
        onCityChange = viewModel::onCityChange,
        onSaveClick = viewModel::onSaveClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyStoreContent(
    state: MyStoreUiState,
    onNameChange: (String) -> Unit,
    onCategoryChange: (StoreCategory) -> Unit,
    onMapTapped: (GeoPoint) -> Unit,
    onStreetChange: (String) -> Unit,
    onPostalCodeChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val center = remember(state.mapCenter) {
        GeoPoint(state.mapCenter.latitude, state.mapCenter.longitude)
    }
    val pin = remember(state.pin) { state.pin?.let { GeoPoint(it.latitude, it.longitude) } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(if (state.isExistingStore) R.string.store_title_edit else R.string.store_title_new),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.store_name)) },
            isError = state.nameError != null,
            supportingText = { ErrorText(state.nameError) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.store_category),
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            StoreCategory.entries.forEach { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { onCategoryChange(category) },
                    label = { Text(stringResource(category.labelRes())) }
                )
            }
        }
        ErrorText(state.categoryError)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.store_location),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = stringResource(R.string.store_location_hint),
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        OsmMap(
            center = center,
            marker = pin,
            markerTitle = stringResource(R.string.store_marker_title),
            onTap = onMapTapped,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        ErrorText(state.pinError)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.store_address),
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = stringResource(R.string.store_address_hint),
            style = MaterialTheme.typography.bodySmall
        )
        if (state.isLookingUpAddress) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = stringResource(R.string.store_looking_up_address),
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = state.street,
            onValueChange = onStreetChange,
            label = { Text(stringResource(R.string.store_street)) },
            isError = state.streetError != null,
            supportingText = { ErrorText(state.streetError) },
            enabled = !state.isLookingUpAddress,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.postalCode,
                onValueChange = onPostalCodeChange,
                label = { Text(stringResource(R.string.store_postal_code)) },
                isError = state.postalCodeError != null,
                supportingText = { ErrorText(state.postalCodeError) },
                enabled = !state.isLookingUpAddress,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.city,
                onValueChange = onCityChange,
                label = { Text(stringResource(R.string.store_city)) },
                isError = state.cityError != null,
                supportingText = { ErrorText(state.cityError) },
                enabled = !state.isLookingUpAddress,
                singleLine = true,
                modifier = Modifier.weight(2f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSaveClick,
            enabled = !state.isSaving && !state.isLookingUpAddress,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(if (state.isExistingStore) R.string.store_save_edit else R.string.store_save_new))
            }
        }
    }
}

@Composable
private fun ErrorText(@StringRes error: Int?) {
    if (error != null) {
        Text(
            text = stringResource(error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@StringRes
private fun StoreCategory.labelRes(): Int = when (this) {
    StoreCategory.BAKERY -> R.string.category_bakery
    StoreCategory.SUPERMARKET -> R.string.category_supermarket
    StoreCategory.RESTAURANT -> R.string.category_restaurant
    StoreCategory.CAFE -> R.string.category_cafe
    StoreCategory.BUTCHER -> R.string.category_butcher
    StoreCategory.GREENGROCER -> R.string.category_greengrocer
    StoreCategory.OTHER -> R.string.category_other
}
