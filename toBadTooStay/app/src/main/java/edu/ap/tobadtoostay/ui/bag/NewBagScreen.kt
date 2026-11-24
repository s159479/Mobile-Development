package edu.ap.tobadtoostay.ui.bag

import androidx.annotation.StringRes
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.ap.tobadtoostay.R

@Composable
fun NewBagScreen(
    snackbarHostState: SnackbarHostState,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewBagViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val messageText = state.message?.let { stringResource(it) }

    LaunchedEffect(messageText) {
        if (messageText != null) {
            snackbarHostState.showSnackbar(messageText)
            viewModel.onMessageShown()
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.loadFailed) {
        Column(modifier = modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.store_error_generic),
                color = MaterialTheme.colorScheme.error
            )
            OutlinedButton(
                onClick = viewModel::loadStore,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.location_retry))
            }
        }
        return
    }

    if (!state.hasStore) {
        Text(
            text = stringResource(R.string.bag_need_store),
            modifier = modifier.padding(24.dp)
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.bag_title_new),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = state.title,
            onValueChange = viewModel::onTitleChange,
            label = { Text(stringResource(R.string.bag_field_title)) },
            isError = state.titleError != null,
            supportingText = { ErrorText(state.titleError) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text(stringResource(R.string.bag_field_description)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.price,
                onValueChange = viewModel::onPriceChange,
                label = { Text(stringResource(R.string.bag_field_price)) },
                isError = state.priceError != null,
                supportingText = { ErrorText(state.priceError) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.originalValue,
                onValueChange = viewModel::onOriginalValueChange,
                label = { Text(stringResource(R.string.bag_field_original_value)) },
                isError = state.originalValueError != null,
                supportingText = { ErrorText(state.originalValueError) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = state.quantity,
            onValueChange = viewModel::onQuantityChange,
            label = { Text(stringResource(R.string.bag_field_quantity)) },
            isError = state.quantityError != null,
            supportingText = { ErrorText(state.quantityError) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = stringResource(R.string.bag_pickup_today),
            style = MaterialTheme.typography.titleSmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.pickupFrom,
                onValueChange = viewModel::onPickupFromChange,
                label = { Text(stringResource(R.string.bag_field_from)) },
                isError = state.pickupFromError != null,
                supportingText = { ErrorText(state.pickupFromError) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = state.pickupUntil,
                onValueChange = viewModel::onPickupUntilChange,
                label = { Text(stringResource(R.string.bag_field_until)) },
                isError = state.pickupUntilError != null,
                supportingText = { ErrorText(state.pickupUntilError) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = viewModel::onSaveClick,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(stringResource(R.string.bag_save))
            }
        }
    }
}

@Composable
private fun ErrorText(@StringRes error: Int?) {
    if (error != null) {
        Text(text = stringResource(error), color = MaterialTheme.colorScheme.error)
    }
}
