package edu.unikom.herbamedjabar.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
    private lateinit var postAdapter: PostAdapter

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
        postAdapter =
            PostAdapter(
                onLikeClicked = { postId -> viewModel.toggleLikeOnPost(postId) },
                onDeleteClicked = { post ->
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Hapus Postingan")
                        .setMessage("Apakah Anda yakin ingin menghapus postingan ini?")
                        .setNegativeButton("Batal", null)
                        .setPositiveButton("Hapus") { _, _ -> viewModel.deletePost(post) }
                        .show()
                },
            )

        binding.rvMyPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            user?.let {
                binding.tvUsername.text = it.displayName ?: "Nama Pengguna"
                binding.tvEmail.text = it.email ?: "Email Pengguna"
                binding.ivProfilePicture.load(it.photoUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_user_image_circular)
                    error(R.drawable.ic_user_image_circular)
                }
            }
        }

        viewModel.userPosts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
            val postCount = posts.size

            // Panggil fungsi untuk update lencana
            updateBadgesVisibility(postCount)

            if (posts.isEmpty()) {
                binding.tvNoPosts.visibility = View.VISIBLE
                binding.rvMyPosts.visibility = View.GONE
            } else {
                binding.tvNoPosts.visibility = View.GONE
                binding.rvMyPosts.visibility = View.VISIBLE
            }
        }
    }

    private fun updateBadgesVisibility(postCount: Int) {
        val badges = listOf(binding.badge1, binding.badge2, binding.badge3, binding.badge4)
        val thresholds =
            listOf(BADGE_THRESHOLD_1, BADGE_THRESHOLD_2, BADGE_THRESHOLD_3, BADGE_THRESHOLD_4)
        badges.forEachIndexed { i, badge ->
            badge.visibility = if (postCount >= thresholds[i]) View.VISIBLE else View.GONE
        }
    }

    private fun handleLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Konfirmasi Logout")
            .setMessage("Apakah Anda yakin ingin logout?")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Ya") { _, _ ->
                viewModel.logout()
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val credentialManager = CredentialManager.create(requireContext())
                        val clearRequest = ClearCredentialStateRequest()
                        credentialManager.clearCredentialState(clearRequest)
                    } catch (e: ClearCredentialException) {
                        Log.e(
                            "ProfileFragment",
                            "Gagal membersihkan kredensial: ${e.localizedMessage}",
                        )
                    } finally {
                        startActivity(Intent(requireContext(), AuthActivity::class.java))
                        activity?.finish()
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val BADGE_THRESHOLD_1 = 1
        private const val BADGE_THRESHOLD_2 = 5
        private const val BADGE_THRESHOLD_3 = 10
        private const val BADGE_THRESHOLD_4 = 20
    }
}
