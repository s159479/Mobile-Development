package edu.ap.tobadtoostay

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import edu.ap.tobadtoostay.ui.theme.ToBadTooStayTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToBadTooStayTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RegisterScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun RegisterScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isMerchant by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf("") }
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
            text = "Account maken",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Naam") },
            isError = nameError != "",
            supportingText = { if (nameError != "") Text(nameError) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

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
            supportingText = {
                if (passwordError != "") {
                    Text(passwordError)
                } else {
                    Text("Minstens 6 tekens, met hoofdletter, kleine letter, cijfer en speciaal teken")
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Switch(checked = isMerchant, onCheckedChange = { isMerchant = it })
            Text(
                text = if (isMerchant) "Ik ben een handelaar" else "Ik ben een klant",
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        if (generalError != "") {
            Text(
                text = generalError,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        Button(
            onClick = {
                // Check the input first
                nameError = ""
                emailError = ""
                passwordError = ""
                generalError = ""

                if (name.isBlank()) {
                    nameError = "Naam is verplicht"
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    emailError = "Dit is geen geldig e-mailadres"
                }
                if (password.length < 6) {
                    passwordError = "Wachtwoord moet minstens 6 tekens hebben"
                } else if (!password.any { it.isUpperCase() }) {
                    passwordError = "Wachtwoord moet een hoofdletter bevatten"
                } else if (!password.any { it.isLowerCase() }) {
                    passwordError = "Wachtwoord moet kleine letters bevatten"
                } else if (!password.any { it.isDigit() }) {
                    passwordError = "Wachtwoord moet een cijfer bevatten"
                } else if (password.all { it.isLetterOrDigit() }) {
                    passwordError = "Wachtwoord moet een speciaal teken bevatten"
                }

                if (nameError == "" && emailError == "" && passwordError == "") {
                    isLoading = true
                    scope.launch {
                        try {
                            // 1. make the account in Firebase Authentication
                            val result = FirebaseAuth.getInstance()
                                .createUserWithEmailAndPassword(email.trim().lowercase(), password)
                                .await()
                            val uid = result.user!!.uid

                            // 2. save name and role in Firestore (users/{uid})
                            val role = if (isMerchant) "merchant" else "customer"
                            val user = hashMapOf(
                                "name" to name.trim(),
                                "email" to email.trim().lowercase(),
                                "role" to role
                            )
                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(uid)
                                .set(user)
                                .await()

                            // 3. go to the main screen
                            // remove login and register from the back stack
                            val intent = Intent(context, MainActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                        } catch (e: FirebaseAuthUserCollisionException) {
                            emailError = "Dit e-mailadres bestaat al"
                        } catch (e: FirebaseNetworkException) {
                            generalError = "Geen internetverbinding"
                        } catch (e: FirebaseAuthException) {
                            if (e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                                emailError = "Dit e-mailadres bestaat al"
                            } else {
                                generalError = "Registreren mislukt: ${e.message}"
                            }
                        } catch (e: Exception) {
                            generalError = "Registreren mislukt: ${e.message}"
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
                Text("Registreren")
            }
        }

        TextButton(
            onClick = { (context as ComponentActivity).finish() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Al een account? Inloggen")
        }
    }
}
