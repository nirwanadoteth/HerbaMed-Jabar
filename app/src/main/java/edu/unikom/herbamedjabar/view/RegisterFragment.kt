package edu.unikom.herbamedjabar.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.databinding.FragmentRegisterBinding
import edu.unikom.herbamedjabar.viewModel.AuthState
import edu.unikom.herbamedjabar.viewModel.AuthViewModel
import java.util.Locale

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

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
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Restore form state from ViewModel
        binding.nameEditText.setText(viewModel.name)
        binding.emailEditText.setText(viewModel.email)
        binding.passwordEditText.setText(viewModel.password)
        binding.confirmPasswordEditText.setText(viewModel.confirmPassword)

        // Save form state to ViewModel on text change
        binding.nameEditText.doAfterTextChanged { viewModel.name = it?.toString() ?: "" }
        binding.emailEditText.doAfterTextChanged { viewModel.email = it?.toString() ?: "" }
        binding.passwordEditText.doAfterTextChanged { viewModel.password = it?.toString() ?: "" }
        binding.confirmPasswordEditText.doAfterTextChanged {
            viewModel.confirmPassword = it?.toString() ?: ""
        }

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.registerButton.setOnClickListener {
            val name = binding.nameEditText.text.toString().trim()
            val email = binding.emailEditText.text.toString().trim().lowercase(Locale.ROOT)
            val password = binding.passwordEditText.text.toString()
            val confirmPassword = binding.confirmPasswordEditText.text.toString()
            viewModel.registerUser(name, email, password, confirmPassword)
        }
        binding.loginTextView.setOnClickListener {
            findNavController(this).navigate(R.id.action_registerFragment_to_loginFragment)
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(viewLifecycleOwner) { state ->
            val loading = state is AuthState.Loading
            binding.loadingIndicator.isVisible = loading
            binding.registerButton.isEnabled = !loading
            binding.nameEditText.isEnabled = !loading
            binding.emailEditText.isEnabled = !loading
            binding.passwordEditText.isEnabled = !loading
            binding.confirmPasswordEditText.isEnabled = !loading
            binding.loginTextView.isEnabled = !loading
            binding.nameInputLayout.isEnabled = !loading
            binding.emailInputLayout.isEnabled = !loading
            binding.passwordInputLayout.isEnabled = !loading
            binding.confirmPasswordInputLayout.isEnabled = !loading

            when (state) {
                is AuthState.Authenticated -> {
                    Snackbar.make(
                        requireView(),
                        getString(R.string.registration_success),
                        Snackbar.LENGTH_SHORT,
                    ).show()
                    // Navigation handled by MainActivity's auth state listener
                }

                is AuthState.Error -> {
                    Snackbar.make(requireView(), state.message, Snackbar.LENGTH_LONG).show()
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
}
