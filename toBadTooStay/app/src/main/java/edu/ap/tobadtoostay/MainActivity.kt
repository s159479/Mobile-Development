package edu.ap.tobadtoostay

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import edu.ap.tobadtoostay.data.repository.UserRepository
import edu.ap.tobadtoostay.ui.location.LocationCard
import edu.ap.tobadtoostay.ui.location.LocationUiState
import edu.ap.tobadtoostay.ui.location.LocationViewModel
import edu.ap.tobadtoostay.ui.nearby.NearbyBagsSummary
import edu.ap.tobadtoostay.ui.theme.ToBadTooStayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToBadTooStayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
    // Only customers look for packages nearby, merchants don't need the location
    val role by produceState<String?>(initialValue = null) {
        value = try {
            UserRepository().getMyRole()
        } catch (e: FirebaseFirestoreException) {
            null // offline and not in the cache: the card shows up once the role is known
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welkom",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(text = "Ingelogd als $email")
        Spacer(modifier = Modifier.height(24.dp))

        if (role == UserRepository.ROLE_CUSTOMER) {
            CustomerSection()
        }

        Button(
            onClick = { context.startActivity(Intent(context, MyStoreActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.my_store))
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                FirebaseAuth.getInstance().signOut()

                // Back to the login screen and remove the other screens from the back stack
                val intent = Intent(context, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Uitloggen")
        }
    }
}

/** Location and the packages nearby, only for customers. */
@Composable
private fun CustomerSection(locationViewModel: LocationViewModel = viewModel()) {
    // Same ViewModel instance as the one inside LocationCard
    val location by locationViewModel.uiState.collectAsStateWithLifecycle()

    LocationCard(viewModel = locationViewModel)
    Spacer(modifier = Modifier.height(16.dp))

    val found = location as? LocationUiState.Found
    if (found != null) {
        NearbyBagsSummary(location = found.point)
        Spacer(modifier = Modifier.height(16.dp))
    }
}
