package edu.ap.tobadtoostay

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import edu.ap.tobadtoostay.ui.BackArrow
import edu.ap.tobadtoostay.ui.bag.NewBagScreen
import edu.ap.tobadtoostay.ui.theme.ToBadTooStayTheme

class NewBagActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToBadTooStayTheme {
                val snackbarHostState = remember { SnackbarHostState() }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        BackArrow(onClick = { finish() })
                        NewBagScreen(
                            snackbarHostState = snackbarHostState,
                            onSaved = {
                                Toast.makeText(this@NewBagActivity, R.string.bag_saved, Toast.LENGTH_SHORT).show()
                                finish()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
