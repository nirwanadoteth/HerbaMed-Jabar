package edu.unikom.herbamedjabar.view

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.databinding.ActivityMainBinding
import edu.unikom.herbamedjabar.viewModel.AuthViewModel
import edu.unikom.herbamedjabar.viewModel.UserAuthState
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: AuthViewModel by viewModels()

    val navController by lazy {
        (supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment)
            .navController
    }

    private val noBottomNavDestinations = setOf(R.id.loginFragment, R.id.registerFragment)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupNavigation()
        observeAuthState()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, systemBars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        NavigationUI.setupWithNavController(binding.navView, navController)

        // Hide bottom nav on specific destinations
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navView.isVisible = destination.id !in noBottomNavDestinations
        }
    }

    private fun navigateToAuthGraph() {
        val navOptions =
            NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true) // Clear back stack
                .build()
        navController.navigate(R.id.loginFragment, null, navOptions)
    }

    private fun navigateToMainGraph() {
        val navOptions =
            NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true) // Clear back stack
                .build()
        navController.navigate(R.id.forumFragment, null, navOptions)
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userAuthState.collect { state ->
                    val currentDest = navController.currentDestination?.id
                    when (state) {
                        is UserAuthState.Authenticated -> {
                            // If user is authenticated and is on a login/register screen, navigate
                            // to main graph
                            if (currentDest in noBottomNavDestinations) {
                                navigateToMainGraph()
                            }
                        }
                        is UserAuthState.Unauthenticated -> {
                            // If user is not authenticated and is NOT on a login/register screen,
                            // navigate to auth graph
                            if (currentDest !in noBottomNavDestinations) {
                                navigateToAuthGraph()
                            }
                        }
                        is UserAuthState.Unknown -> {
                            // Do nothing, wait for state to be determined
                        }
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
