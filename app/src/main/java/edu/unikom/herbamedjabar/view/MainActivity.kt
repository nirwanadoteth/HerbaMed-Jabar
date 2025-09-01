package edu.unikom.herbamedjabar.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.databinding.ActivityMainBinding
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @Inject
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        if (auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }
        if (savedInstanceState == null) {
            setCurrentFragment(ForumFragment(), false)
        }
        binding.navView.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.navigation_scan -> ScanFragment()
                R.id.navigation_forum -> ForumFragment()
                R.id.navigation_history -> HistoryFragment()
                R.id.navigation_profile -> ProfileFragment()
                else -> null
            }
            fragment?.let {
                setCurrentFragment(it, false)
                true
            } ?: false
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                binding.navView.visibility = View.GONE
            } else {
                binding.navView.visibility = View.VISIBLE
            }
        }
    }

    private fun setCurrentFragment(fragment: Fragment, addToBackStack: Boolean) {
        val transaction =
            supportFragmentManager.beginTransaction().replace(R.id.nav_host_fragment, fragment)

        if (addToBackStack) {
            transaction.addToBackStack(null)
        }

        transaction.commit()
    }

    fun showResultFragment(
        imagePath: String,
        resultText: String,
        plantName: String,
        benefit: String,
        warning: String,
        content: String
    ) {
        val resultFragment = ResultFragment.newInstance(
            imagePath,
            resultText,
            plantName,
            benefit,
            warning,
            content
        )
        setCurrentFragment(resultFragment, true)
    }

    fun showHistoryDetailFragment(history: edu.unikom.herbamedjabar.data.ScanHistory) {
        val historyDetailFragment = HistoryDetailFragment.newInstance(history)
        setCurrentFragment(historyDetailFragment, true)
    }
}
