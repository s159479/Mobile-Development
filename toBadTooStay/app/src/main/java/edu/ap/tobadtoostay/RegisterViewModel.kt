package edu.ap.tobadtoostay

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Everything the register screen shows. The errors are ids of texts in strings.xml. */
data class RegisterUiState(
    @StringRes val nameError: Int? = null,
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isLoading: Boolean = false,
    val isRegistered: Boolean = false
)

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun register(name: String, email: String, password: String, isMerchant: Boolean) {
        val cleanName = name.trim()
        val cleanEmail = email.trim().lowercase()

        // Check the input first
        val nameError = if (cleanName == "") R.string.error_name_required else null
        val emailError = if (Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) null else R.string.error_email_invalid
        val passwordError = checkPassword(password)
        _uiState.value = RegisterUiState(nameError = nameError, emailError = emailError, passwordError = passwordError)
        if (nameError != null || emailError != null || passwordError != null) return

        _uiState.value = RegisterUiState(isLoading = true)
        viewModelScope.launch {
            try {
                // 1. make the account in Firebase Authentication
                val result = FirebaseAuth.getInstance()
                    .createUserWithEmailAndPassword(cleanEmail, password)
                    .await()
                val uid = result.user!!.uid

                // 2. save name, email and role in Firestore (users/{uid})
                val user = hashMapOf(
                    "name" to cleanName,
                    "email" to cleanEmail,
                    "role" to if (isMerchant) "merchant" else "customer"
                )
                FirebaseFirestore.getInstance().collection("users").document(uid).set(user).await()

                _uiState.value = RegisterUiState(isRegistered = true)
            } catch (e: FirebaseAuthUserCollisionException) {
                _uiState.value = RegisterUiState(emailError = R.string.error_email_in_use)
            } catch (e: FirebaseNetworkException) {
                _uiState.value = RegisterUiState(generalError = R.string.error_no_internet)
            } catch (e: FirebaseAuthException) {
                if (e.errorCode == "ERROR_EMAIL_ALREADY_IN_USE") {
                    _uiState.value = RegisterUiState(emailError = R.string.error_email_in_use)
                } else {
                    _uiState.value = RegisterUiState(generalError = R.string.error_register_failed)
                }
            } catch (e: Exception) {
                _uiState.value = RegisterUiState(generalError = R.string.error_register_failed)
            }
        }
    }

    /** At least 6 characters, with a capital, a small letter, a number and a special sign. */
    private fun checkPassword(password: String): Int? {
        return if (password.length < 6) {
            R.string.error_password_short
        } else if (!password.any { it.isUpperCase() }) {
            R.string.error_password_upper
        } else if (!password.any { it.isLowerCase() }) {
            R.string.error_password_lower
        } else if (!password.any { it.isDigit() }) {
            R.string.error_password_digit
        } else if (password.all { it.isLetterOrDigit() }) {
            R.string.error_password_special
        } else {
            null
        }
    }
}
