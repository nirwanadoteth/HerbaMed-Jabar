package edu.unikom.herbamedjabar.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
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
        binding.loginTextView.setOnClickListener { parentFragmentManager.popBackStack() }
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
                    Toast.makeText(
                            requireContext(),
                            getString(R.string.registration_success),
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
}
