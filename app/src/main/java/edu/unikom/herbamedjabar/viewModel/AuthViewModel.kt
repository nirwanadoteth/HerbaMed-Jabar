package edu.unikom.herbamedjabar.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

private fun Throwable.toUserMessage(fallback: String): String = when (this) {
    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Email atau password salah."
    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Akun tidak ditemukan atau dinonaktifkan."
    is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Email sudah terdaftar."
    is com.google.firebase.FirebaseTooManyRequestsException -> "Terlalu banyak percobaan. Coba lagi nanti."
    is com.google.firebase.FirebaseNetworkException -> "Masalah koneksi internet."
    is kotlinx.coroutines.TimeoutCancellationException -> "Permintaan waktu habis. Coba lagi nanti."
    else -> fallback
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun loginUser(email: String, password: String) {
        val emailT = email.trim()
        val validation = validateLoginInput(emailT, password)
        if (validation != null) {
            _authState.value = AuthState.Error(validation)
            return
        }
        runAuthOp("Login") {
            firebaseAuth.signInWithEmailAndPassword(emailT, password).await()
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        val validation = validateGoogleToken(idToken)
        if (validation != null) {
            Log.w(TAG, "signInWithGoogleToken: idToken is blank")
            _authState.value = AuthState.Error(validation)
            return
        }
        runAuthOp("Login dengan Google") {
            withTimeout(GOOGLE_LOGIN_TIMEOUT) {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential).await()
            }
        }
    }

    fun registerUser(name: String, email: String, password: String, confirmPassword: String) {
        val nameT = name.trim()
        val emailT = email.trim()
        val validation = validateRegistrationInput(nameT, emailT, password, confirmPassword)
        if (validation != null) {
            _authState.value = AuthState.Error(validation)
            return
        }
        runAuthOp("Registrasi") {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(emailT, password).await()
            val user = authResult.user ?: error("User tidak tersedia setelah registrasi")
            val profileUpdates = userProfileChangeRequest { displayName = nameT }
            user.updateProfile(profileUpdates).await()
            Log.d(TAG, "User profile updated.")
        }
    }

    // Validation helpers
    private fun validateLoginInput(email: String, password: String): String? {
        return if (email.isBlank() || password.isBlank()) {
            "Email dan password tidak boleh kosong."
        } else {
            null
        }
    }

    private fun validateGoogleToken(idToken: String): String? {
        return if (idToken.isBlank()) "Token Google tidak valid." else null
    }

    private fun validateRegistrationInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String
    ): String? {
        var error: String? = null
        when {
            listOf(name, email, password, confirmPassword).any { it.isBlank() } ->
                error = "Semua kolom harus diisi."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                error = "Email tidak valid."
            password != confirmPassword ->
                error = "Password dan konfirmasi password tidak cocok."
            password.length < MIN_PASSWORD_LENGTH ->
                error = "Password minimal harus $MIN_PASSWORD_LENGTH karakter."
        }
        return error
    }

    private inline fun runAuthOp(
        opName: String,
        crossinline block: suspend () -> Unit
    ) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            block()
            Log.d(TAG, "$opName: Authenticated")
            _authState.value = AuthState.Authenticated
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "$opName: Timeout", e)
            _authState.value = AuthState.Error(e.toUserMessage("$opName gagal"))
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.w(TAG, "$opName: Error", t)
            _authState.value = AuthState.Error(t.toUserMessage("$opName gagal"))
        }
    }

    companion object {
        private const val GOOGLE_LOGIN_TIMEOUT = 15_000L
        private const val MIN_PASSWORD_LENGTH = 6
        private const val TAG = "AuthViewModel"
    }
}
