package edu.unikom.herbamedjabar.util

object ValidationUtils {
    private const val MIN_PASSWORD_LENGTH = 6

    fun validateLoginInput(email: String, password: String): String? {
        return if (email.isBlank() || password.isBlank()) {
            "Email dan password tidak boleh kosong."
        } else {
            null
        }
    }

    fun validateGoogleToken(idToken: String): String? {
        return if (idToken.isBlank()) "Token Google tidak valid." else null
    }

    fun validateRegistrationInput(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
    ): String? {
        return when {
            listOf(name, email, password, confirmPassword).any { it.isBlank() } ->
                "Semua kolom harus diisi."
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Email tidak valid."
            password != confirmPassword -> "Password dan konfirmasi password tidak cocok."
            password.length < MIN_PASSWORD_LENGTH ->
                "Password minimal harus $MIN_PASSWORD_LENGTH karakter."
            else -> null
        }
    }
}
