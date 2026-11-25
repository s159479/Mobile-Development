package edu.ap.tobadtoostay.ui.orders

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.data.model.Order
import edu.ap.tobadtoostay.data.model.OrderStatus
import edu.ap.tobadtoostay.ui.PickupCode
import edu.ap.tobadtoostay.ui.formatDay
import edu.ap.tobadtoostay.ui.formatTime

/** Tab "Bestellingen": active and past reservations with their status (K10). */
@Composable
fun OrdersScreen(state: OrdersUiState, modifier: Modifier = Modifier) {
    when (state) {
        OrdersUiState.Loading -> {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.orders_loading),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        is OrdersUiState.Loaded -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.orders_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                syncMessage(state.syncStatus)?.let { message ->
                    item {
                        Text(
                            text = stringResource(message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.active.isEmpty() && state.past.isEmpty()) {
                    item { Text(stringResource(R.string.orders_empty)) }
                } else {
                    item { SectionTitle(R.string.orders_active) }
                    if (state.active.isEmpty()) {
                        item { Text(stringResource(R.string.orders_active_empty)) }
                    }
                    items(state.active, key = { it.reservation.id }) { OrderCard(it) }

                    if (state.past.isNotEmpty()) {
                        item { SectionTitle(R.string.orders_past) }
                        items(state.past, key = { it.reservation.id }) { OrderCard(it) }
                    }
                }
            }
        }
    }
}

@StringRes
private fun syncMessage(status: SyncStatus): Int? = when (status) {
    SyncStatus.UP_TO_DATE -> null
    SyncStatus.OFFLINE -> R.string.orders_offline
    SyncStatus.FAILED -> R.string.orders_sync_failed
}

@Composable
private fun SectionTitle(@StringRes titleRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun OrderCard(order: Order) {
    val reservation = order.reservation

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (order.isActive) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = reservation.bagTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                StatusLabel(order.status)
            }
            Text(
                text = reservation.storeName,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(R.string.orders_quantity, reservation.quantity),
                style = MaterialTheme.typography.bodySmall
            )
            if (reservation.pickupStart > 0 && reservation.pickupEnd > 0) {
                Text(
                    text = stringResource(
                        R.string.orders_pickup,
                        formatDay(reservation.pickupStart),
                        formatTime(reservation.pickupStart),
                        formatTime(reservation.pickupEnd)
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // the code is only needed while the order still has to be collected
            if (order.isActive) {
                Text(
                    text = stringResource(R.string.reserve_show_code),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
                PickupCode(reservation.pickupCode, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun StatusLabel(status: OrderStatus) {
    val (text, color) = when (status) {
        // only shown on an active card, which has the primaryContainer background
        OrderStatus.RESERVED -> R.string.order_status_reserved to MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.COLLECTED -> R.string.order_status_collected to MaterialTheme.colorScheme.tertiary
        OrderStatus.CANCELLED -> R.string.order_status_cancelled to MaterialTheme.colorScheme.onSurfaceVariant
        OrderStatus.EXPIRED -> R.string.order_status_expired to MaterialTheme.colorScheme.error
    }
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier.padding(start = 8.dp)
    )
}
