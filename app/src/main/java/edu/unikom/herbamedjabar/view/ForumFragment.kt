package edu.unikom.herbamedjabar.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.adapter.PostAdapter
import edu.unikom.herbamedjabar.databinding.FragmentForumBinding
import edu.unikom.herbamedjabar.viewModel.ForumViewModel

@AndroidEntryPoint
class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ForumViewModel by viewModels()
    private lateinit var postAdapter: PostAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentForumBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()

        binding.scanButton.setOnClickListener {
            val bottomNav = activity?.findViewById<BottomNavigationView>(R.id.nav_view)
            bottomNav?.selectedItemId = R.id.navigation_scan
        }
    }

    private fun setupRecyclerView() {
        postAdapter = PostAdapter(
            onLikeClicked = { postId ->
                viewModel.toggleLikeOnPost(postId)
            },
            onDeleteClicked = { post ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Hapus Postingan")
                    .setMessage("Apakah Anda yakin ingin menghapus postingan ini?")
                    .setNegativeButton("Batal", null)
                    .setPositiveButton("Hapus") { _, _ ->
                        viewModel.deletePost(post)
                    }
                    .show()
            }
        )

        binding.rvPosts.apply {
            adapter = postAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts ->
            postAdapter.submitList(posts)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
