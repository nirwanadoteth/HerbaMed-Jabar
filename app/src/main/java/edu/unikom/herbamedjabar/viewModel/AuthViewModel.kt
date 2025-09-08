package edu.unikom.herbamedjabar.viewModel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.userProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import javax.inject.Inject

sealed class AuthOperationState {
    object Idle : AuthOperationState()

    object Loading : AuthOperationState()

    object Authenticated : AuthOperationState()

    data class Error(val message: String) : AuthOperationState()
}

sealed class UserAuthState {
    object Unknown : UserAuthState() // Initial state before Firebase check

    data class Authenticated(val user: FirebaseUser) : UserAuthState()

    object Unauthenticated : UserAuthState()
}

private fun Throwable.toUserMessage(fallback: String): String =
    when (this) {
        is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException,
        is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Email atau password salah."
        is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "Email sudah terdaftar."
        is com.google.firebase.FirebaseTooManyRequestsException ->
            "Terlalu banyak percobaan. Coba lagi nanti."
        is com.google.firebase.FirebaseNetworkException -> "Masalah koneksi internet."
        is kotlinx.coroutines.TimeoutCancellationException ->
            "Permintaan waktu habis. Coba lagi nanti."
        else -> fallback
    }

@HiltViewModel
class AuthViewModel
@Inject
constructor(
    private val firebaseAuth: FirebaseAuth,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _authOperationState = MutableLiveData<AuthOperationState>(AuthOperationState.Idle)
    val authOperationState: LiveData<AuthOperationState> = _authOperationState

    private val _userAuthState = MutableStateFlow<UserAuthState>(UserAuthState.Unknown)
    val userAuthState: StateFlow<UserAuthState> = _userAuthState

    private val authStateListener =
        FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            _userAuthState.value =
                if (user != null) {
                    UserAuthState.Authenticated(user)
                } else {
                    UserAuthState.Unauthenticated
                }
            Log.d(
                TAG,
                "Auth state changed. User is ${if (user != null) "Authenticated" else "Unauthenticated"}",
            )
        }

    init {
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onCleared() {
        super.onCleared()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }

    var email: String
        get() = savedStateHandle["email"] ?: ""
        set(value) {
            savedStateHandle["email"] = value
        }

    var password: String
        get() = savedStateHandle["password"] ?: ""
        set(value) {
            savedStateHandle["password"] = value
        }

    var name: String
        get() = savedStateHandle["name"] ?: ""
        set(value) {
            savedStateHandle["name"] = value
        }

    var confirmPassword: String
        get() = savedStateHandle["confirmPassword"] ?: ""
        set(value) {
            savedStateHandle["confirmPassword"] = value
        }

    private val opInProgress = java.util.concurrent.atomic.AtomicBoolean(false)

    fun loginUser(email: String, password: String) {
        val emailT = email.trim()
        val validation = validateLoginInput(emailT, password)
        if (validation != null) {
            _authOperationState.value = AuthOperationState.Error(validation)
            return
        }
        runAuthOp("Login") {
            withTimeout(AUTH_TIMEOUT_MS) {
                firebaseAuth.signInWithEmailAndPassword(emailT, password).await()
            }
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        val validation = validateGoogleToken(idToken)
        if (validation != null) {
            Log.w(TAG, "signInWithGoogleToken: idToken is blank")
            _authOperationState.value = AuthOperationState.Error(validation)
            return
        }
        runAuthOp("Login dengan Google") {
            withTimeout(AUTH_TIMEOUT_MS) {
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
            _authOperationState.value = AuthOperationState.Error(validation)
            return
        }
        runAuthOp("Registrasi") {
            withTimeout(AUTH_TIMEOUT_MS) {
                val authResult =
                    firebaseAuth.createUserWithEmailAndPassword(emailT, password).await()
                val user =
                    requireNotNull(authResult.user) { "User tidak tersedia setelah registrasi" }
                val profileUpdates = userProfileChangeRequest { displayName = nameT }
                user.updateProfile(profileUpdates).await()
            }
            Log.d(TAG, "User profile updated.")
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

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
        confirmPassword: String,
    ): String? {
        var error: String? = null
        when {
            listOf(name, email, password, confirmPassword).any { it.isBlank() } ->
                error = "Semua kolom harus diisi."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                error = "Email tidak valid."
            password != confirmPassword -> error = "Password dan konfirmasi password tidak cocok."
            password.length < MIN_PASSWORD_LENGTH ->
                error = "Password minimal harus $MIN_PASSWORD_LENGTH karakter."
        }
        return error
    }

    private inline fun runAuthOp(opName: String, crossinline block: suspend () -> Unit) =
        viewModelScope.launch {
            if (!opInProgress.compareAndSet(false, true)) {
                Log.d(TAG, "$opName: Ignored because another auth op is in progress")
                return@launch
            }
            _authOperationState.value = AuthOperationState.Loading
            try {
                block()
                Log.d(TAG, "$opName: Authenticated")
                _authOperationState.value = AuthOperationState.Authenticated
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "$opName: Timeout", e)
                _authOperationState.value =
                    AuthOperationState.Error(e.toUserMessage("$opName gagal"))
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "$opName: Error", e)
                _authOperationState.value =
                    AuthOperationState.Error(e.toUserMessage("$opName gagal"))
            } finally {
                opInProgress.set(false)
            }
        }

    companion object {
        private const val AUTH_TIMEOUT_MS = 30_000L
        private const val MIN_PASSWORD_LENGTH = 6
        private const val TAG = "AuthViewModel"
    }
}
