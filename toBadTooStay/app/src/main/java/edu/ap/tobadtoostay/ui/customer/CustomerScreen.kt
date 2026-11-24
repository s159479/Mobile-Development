package edu.ap.tobadtoostay.ui.customer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import edu.ap.tobadtoostay.R
import edu.ap.tobadtoostay.ui.BackArrow
import edu.ap.tobadtoostay.ui.detail.BagDetailScreen
import edu.ap.tobadtoostay.ui.detail.ReservationViewModel
import edu.ap.tobadtoostay.ui.location.LocationCard
import edu.ap.tobadtoostay.ui.location.LocationUiState
import edu.ap.tobadtoostay.ui.location.LocationViewModel
import edu.ap.tobadtoostay.ui.nearby.NearbyBagsUiState
import edu.ap.tobadtoostay.ui.nearby.NearbyListScreen
import edu.ap.tobadtoostay.ui.nearby.NearbyLiveViewModel
import edu.ap.tobadtoostay.ui.nearby.NearbyMapScreen

private const val DETAIL_ROUTE = "detail/{bagId}"

/** The four tabs of the customer: route, text and icon. */
private enum class CustomerTab(val route: String, val label: Int, val icon: ImageVector) {
    DISCOVER("discover", R.string.tab_discover, Icons.Filled.Home),
    MAP("map", R.string.tab_map, Icons.Filled.Place),
    ORDERS("orders", R.string.tab_orders, Icons.Filled.ShoppingCart),
    FAVORITES("favorites", R.string.tab_favorites, Icons.Filled.Favorite)
}

/** Start screen of a customer: bottom navigation bar with Ontdekken, Kaart, Bestellingen and Favorieten. */
@Composable
fun CustomerScreen(
    onLogout: () -> Unit,
    locationViewModel: LocationViewModel = viewModel(),
    nearbyViewModel: NearbyLiveViewModel = viewModel()
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // The location and the bags are shared by the list and the map
    val location by locationViewModel.uiState.collectAsStateWithLifecycle()
    val nearbyState by nearbyViewModel.uiState.collectAsStateWithLifecycle()
    val found = location as? LocationUiState.Found

    val openDetail: (String) -> Unit = { bagId -> navController.navigate("detail/$bagId") }

    LaunchedEffect(found?.point) {
        if (found != null) nearbyViewModel.load(found.point)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                CustomerTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(CustomerTab.DISCOVER.route)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.label)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = CustomerTab.DISCOVER.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(CustomerTab.DISCOVER.route) {
                Column(modifier = Modifier.padding(16.dp)) {
                    LogoutButton(onLogout)
                    if (found == null) {
                        LocationCard(viewModel = locationViewModel)
                    } else {
                        NearbyListScreen(
                            state = nearbyState,
                            onRetry = nearbyViewModel::retry,
                            onBagClick = openDetail
                        )
                    }
                }
            }
            composable(CustomerTab.MAP.route) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BackArrow(onClick = { navController.popBackStack() })
                    if (found == null) {
                        LocationCard(viewModel = locationViewModel)
                    } else {
                        NearbyMapScreen(
                            state = nearbyState,
                            location = found.point,
                            onRetry = nearbyViewModel::retry,
                            onBagClick = openDetail
                        )
                    }
                }
            }
            composable(DETAIL_ROUTE) { entry ->
                val bagId = entry.arguments?.getString("bagId")
                val nearbyBag = (nearbyState as? NearbyBagsUiState.Loaded)
                    ?.bags?.firstOrNull { it.bag.id == bagId }
                // own ViewModel per detail screen, so every bag starts without a reservation
                val reservationViewModel: ReservationViewModel = viewModel()
                val reservationState by reservationViewModel.uiState.collectAsStateWithLifecycle()

                BagDetailScreen(
                    nearbyBag = nearbyBag,
                    isLoading = nearbyState is NearbyBagsUiState.Loading ||
                        nearbyState is NearbyBagsUiState.WaitingForLocation,
                    reservationState = reservationState,
                    onReserve = { quantity ->
                        if (nearbyBag != null) reservationViewModel.reserve(nearbyBag, quantity)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(CustomerTab.ORDERS.route) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BackArrow(onClick = { navController.popBackStack() })
                    Text(stringResource(R.string.coming_soon))
                }
            }
            composable(CustomerTab.FAVORITES.route) {
                Column(modifier = Modifier.padding(16.dp)) {
                    BackArrow(onClick = { navController.popBackStack() })
                    Text(stringResource(R.string.coming_soon))
                }
            }
        }
    }
}

@Composable
private fun LogoutButton(onLogout: () -> Unit) {
    TextButton(onClick = onLogout) {
        Text(stringResource(R.string.logout))
    }
}
