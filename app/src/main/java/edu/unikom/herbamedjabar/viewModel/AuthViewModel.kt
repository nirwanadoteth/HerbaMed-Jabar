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
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Authenticated : AuthState()
    data class Error(val message: String) : AuthState()
}

private const val TAG = "AuthViewModel"
private fun Throwable.toUserMessage(fallback: String): String = when (this) {
    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Email atau password salah."
    is com.google.firebase.auth.FirebaseAuthInvalidUserException -> "Akun tidak ditemukan atau dinonaktifkan."
    is com.google.firebase.FirebaseTooManyRequestsException -> "Terlalu banyak percobaan. Coba lagi nanti."
    is com.google.firebase.FirebaseNetworkException -> "Masalah koneksi internet."
    else -> fallback
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _authState = MutableLiveData<AuthState>(AuthState.Idle)
    val authState: LiveData<AuthState> = _authState

    fun loginUser(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email dan password tidak boleh kosong.")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = runCatching {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
            }
            _authState.value = if (result.isSuccess) {
                Log.d(TAG, "loginUser: Authenticated")
                AuthState.Authenticated
            } else {
                Log.w(TAG, "loginUser: Error", result.exceptionOrNull())
                AuthState.Error(result.exceptionOrNull()?.toUserMessage("Login gagal") ?: "Login gagal")
            }
        }
    }

    fun signInWithGoogleToken(idToken: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            if (idToken.isBlank()) {
                Log.w(TAG, "signInWithGoogleToken: idToken is blank")
                _authState.value = AuthState.Error("Token Google tidak valid.")
                return@launch
            }
            val result = runCatching {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                firebaseAuth.signInWithCredential(credential).await()
            }
            _authState.value = if (result.isSuccess) {
                AuthState.Authenticated
            } else {
                Log.w(TAG, "signInWithGoogleToken: error", result.exceptionOrNull())
                AuthState.Error(result.exceptionOrNull()?.toUserMessage("Login dengan Google gagal") ?: "Login dengan Google gagal")
            }
        }
    }

    fun registerUser(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _authState.value = AuthState.Error("Semua kolom harus diisi.")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Password dan konfirmasi password tidak cocok.")
            return
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            _authState.value = AuthState.Error("Password minimal harus $MIN_PASSWORD_LENGTH karakter.")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = runCatching {
                val authResult =
                    firebaseAuth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user ?: error("User tidak tersedia setelah registrasi")
                val profileUpdates = userProfileChangeRequest { displayName = name }
                user.updateProfile(profileUpdates).await()
                Log.d(TAG, "User profile updated.")
            }
            _authState.value = if (result.isSuccess) {
                AuthState.Authenticated
            } else {
                AuthState.Error(result.exceptionOrNull()?.message ?: "Registrasi gagal")
            }
        }
    }
    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
