package edu.ap.tobadtoostay

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import edu.ap.tobadtoostay.ui.theme.ToBadTooStayTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If the user is still logged in, skip the login screen
        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            ToBadTooStayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    LoginScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var generalError by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Inloggen",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            isError = emailError != "",
            supportingText = { if (emailError != "") Text(emailError) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Wachtwoord") },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordError != "",
            supportingText = { if (passwordError != "") Text(passwordError) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (generalError != "") {
            Text(
                text = generalError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = {
                emailError = ""
                passwordError = ""
                generalError = ""

                if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    emailError = "Dit is geen geldig e-mailadres"
                }
                if (password == "") {
                    passwordError = "Wachtwoord is verplicht"
                }

                if (emailError == "" && passwordError == "") {
                    isLoading = true
                    scope.launch {
                        try {
                            FirebaseAuth.getInstance()
                                .signInWithEmailAndPassword(email.trim().lowercase(), password)
                                .await()

                            context.startActivity(Intent(context, MainActivity::class.java))
                            (context as ComponentActivity).finish()
                        } catch (e: FirebaseAuthInvalidUserException) {
                            generalError = "E-mail of wachtwoord is fout"
                        } catch (e: FirebaseAuthInvalidCredentialsException) {
                            generalError = "E-mail of wachtwoord is fout"
                        } catch (e: FirebaseNetworkException) {
                            generalError = "Geen internetverbinding"
                        } catch (e: Exception) {
                            generalError = "Inloggen mislukt: ${e.message}"
                        }
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Inloggen")
            }
        }

        TextButton(
            onClick = { context.startActivity(Intent(context, RegisterActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Nog geen account? Registreren")
        }
    }
}
