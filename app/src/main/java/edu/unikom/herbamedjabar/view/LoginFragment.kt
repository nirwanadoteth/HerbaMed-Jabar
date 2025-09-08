package edu.unikom.herbamedjabar.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.databinding.FragmentLoginBinding
import edu.unikom.herbamedjabar.viewModel.AuthState
import edu.unikom.herbamedjabar.viewModel.AuthViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding: FragmentLoginBinding
        get() = _binding ?: error("Binding is only valid between onCreateView and onDestroyView")

    private val viewModel: AuthViewModel by viewModels()

    private val credentialManager: CredentialManager by
        lazy(LazyThreadSafetyMode.NONE) { CredentialManager.create(requireContext()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore form state from ViewModel
        binding.emailEditText.setText(viewModel.email)
        binding.passwordEditText.setText(viewModel.password)

        // Save form state to ViewModel on text change
        binding.emailEditText.doAfterTextChanged { viewModel.email = it?.toString() ?: "" }
        binding.passwordEditText.doAfterTextChanged { viewModel.password = it?.toString() ?: "" }

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.loginButton.setOnClickListener {
            val email =
                binding.emailEditText.text.toString().trim().lowercase(java.util.Locale.ROOT)
            val password = binding.passwordEditText.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                binding.emailInputLayout.error =
                    if (email.isEmpty()) getString(R.string.error_email_required) else null
                binding.passwordInputLayout.error =
                    if (password.isEmpty()) getString(R.string.error_password_required) else null
            } else {
                binding.emailInputLayout.error = null
                binding.passwordInputLayout.error = null
                viewModel.loginUser(email, password)
            }
        }

        binding.googleLoginButton.setOnClickListener { launchGoogleSignIn() }

        binding.registerTextView.setOnClickListener {
            findNavController(this).navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    private fun launchGoogleSignIn() {
        // Create the dialog configuration for the Credential Manager request
        val signInWithGoogleOption =
            GetSignInWithGoogleOption.Builder(
                    serverClientId = requireContext().getString(R.string.default_web_client_id)
                )
                .build()

        // Create the Credential Manager request using the configuration created above
        val request =
            GetCredentialRequest.Builder().addCredentialOption(signInWithGoogleOption).build()

        launchCredentialManager(request)
    }

    private fun launchCredentialManager(request: GetCredentialRequest) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val credential =
                    credentialManager
                        .getCredential(context = requireContext(), request = request)
                        .credential
                createGoogleIdToken(credential)
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.no_credentials_available),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                Log.w(TAG, "NoCredentialException: ${e.localizedMessage}")
            } catch (e: androidx.credentials.exceptions.GetCredentialException) {
                Log.e(TAG, "Gagal mendapatkan kredensial pengguna: ${e.localizedMessage}")
                Toast.makeText(
                        requireContext(),
                        getString(R.string.credential_error_generic),
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
        }
    }

    private fun createGoogleIdToken(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                viewModel.signInWithGoogleToken(googleIdTokenCredential.idToken)
            } catch (e: IllegalArgumentException) {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.google_token_invalid),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
                Log.w(TAG, "Invalid Google ID token credential", e)
            }
        } else {
            Log.w(TAG, "Kredensial tidak sesuai dengan Google ID Token")
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            val loading = state is AuthState.Loading
            binding.loadingIndicator.isVisible = loading
            binding.loginButton.isEnabled = !loading
            binding.googleLoginButton.isEnabled = !loading
            binding.emailEditText.isEnabled = !loading
            binding.passwordEditText.isEnabled = !loading
            binding.registerTextView.isEnabled = !loading
            binding.emailInputLayout.isEnabled = !loading
            binding.passwordInputLayout.isEnabled = !loading

            when (state) {
                is AuthState.Authenticated -> {
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.login_success),
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                    // Pindah ke MainActivity dan bersihkan back stack
                    val intent = Intent(requireActivity(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }

                is AuthState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                }

                else -> {
                    // Idle state
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "LoginFragment"
    }
}
