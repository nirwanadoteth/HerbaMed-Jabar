package edu.unikom.herbamedjabar.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.adapter.PostAdapter
import edu.unikom.herbamedjabar.databinding.FragmentProfileBinding
import edu.unikom.herbamedjabar.viewModel.ProfileViewModel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private var postAdapter: PostAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.btnLogout.setOnClickListener { handleLogout() }
    }

    private fun setupRecyclerView() {
        val adapterObj =
            PostAdapter(
                onLikeClicked = { postId -> viewModel.toggleLikeOnPost(postId) },
                onDeleteClicked = { post ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.delete_post_title))
                        .setMessage(getString(R.string.delete_post_message))
                        .setNegativeButton(getString(R.string.action_cancel), null)
                        .setPositiveButton(getString(R.string.action_delete)) { _, _ -> viewModel.deletePost(post) }
                        .show()
                },
                currentUser = viewModel.getCurrentUser()
            )
        postAdapter = adapterObj
        binding.rvMyPosts.apply {
            adapter = adapterObj
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvUsername.text = it.displayName ?: getString(R.string.default_username)
                binding.tvEmail.text = it.email ?: getString(R.string.default_email)
                binding.ivProfilePicture.load(it.photoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.avatar)
                    error(R.drawable.avatar)
                }
            }
        }

        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            postAdapter?.submitList(posts)
            val postCount = posts.size

            // Panggil fungsi untuk update lencana
            updateBadgesVisibility(postCount)

            binding.tvNoPosts.isVisible = posts.isEmpty()
            binding.rvMyPosts.isVisible = posts.isNotEmpty()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingIndicator.isVisible = isLoading == true
        }
    }

    private fun updateBadgesVisibility(postCount: Int) {
        val badges = listOf(binding.badge1, binding.badge2, binding.badge3, binding.badge4)
        val thresholds =
            listOf(BADGE_THRESHOLD_1, BADGE_THRESHOLD_2, BADGE_THRESHOLD_3, BADGE_THRESHOLD_4)
        badges.forEachIndexed { i, badge ->
            badge.isVisible = postCount >= thresholds[i]
        }
    }

    private fun handleLogout() {
        val appCtx = requireContext().applicationContext
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.logout_title))
            .setMessage(getString(R.string.logout_message))
            .setNegativeButton(getString(R.string.action_cancel), null)
            .setPositiveButton(getString(R.string.action_logout)) { _, _ ->
                viewModel.logout()
                lifecycleScope.launch {
                    try {
                        val credentialManager = CredentialManager.create(appCtx)
                        val clearRequest = ClearCredentialStateRequest()
                        credentialManager.clearCredentialState(clearRequest)
                    } catch (e: ClearCredentialException) {
                        Log.e(
                            "ProfileFragment",
                            "Gagal membersihkan kredensial: ${e.localizedMessage}",
                        )
                    } finally {
                        val intent =
                            Intent(appCtx, AuthActivity::class.java).apply {
                                flags =
                                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        appCtx.startActivity(intent)
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvMyPosts.adapter = null
        postAdapter = null
        _binding = null
    }

    companion object {
        private const val BADGE_THRESHOLD_1 = 1
        private const val BADGE_THRESHOLD_2 = 5
        private const val BADGE_THRESHOLD_3 = 10
        private const val BADGE_THRESHOLD_4 = 20
    }
}
