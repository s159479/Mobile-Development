package edu.ap.tobadtoostay

import android.util.Patterns
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Everything the login screen shows. The errors are ids of texts in strings.xml. */
data class LoginUiState(
    @StringRes val emailError: Int? = null,
    @StringRes val passwordError: Int? = null,
    @StringRes val generalError: Int? = null,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        val cleanEmail = email.trim().lowercase()

        // Check the input first
        val emailError = if (Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) null else R.string.error_email_invalid
        val passwordError = if (password == "") R.string.error_password_required else null
        _uiState.value = LoginUiState(emailError = emailError, passwordError = passwordError)
        if (emailError != null || passwordError != null) return

        _uiState.value = LoginUiState(isLoading = true)
        viewModelScope.launch {
            try {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(cleanEmail, password).await()
                _uiState.value = LoginUiState(isLoggedIn = true)
            } catch (e: FirebaseAuthInvalidUserException) {
                _uiState.value = LoginUiState(generalError = R.string.error_login_wrong)
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                _uiState.value = LoginUiState(generalError = R.string.error_login_wrong)
            } catch (e: FirebaseNetworkException) {
                _uiState.value = LoginUiState(generalError = R.string.error_no_internet)
            } catch (e: Exception) {
                _uiState.value = LoginUiState(generalError = R.string.error_login_failed)
            }
        }
    }
}
