package edu.unikom.herbamedjabar.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.adapter.PostAdapter
import edu.unikom.herbamedjabar.databinding.FragmentForumBinding
import edu.unikom.herbamedjabar.viewModel.ForumViewModel

@AndroidEntryPoint
class ForumFragment : Fragment() {

    private var _binding: FragmentForumBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: ForumViewModel by viewModels()
    private var postAdapter: PostAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        returnTransition = MaterialFadeThrough()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
            bottomNav?.selectedItemId = R.id.scanFragment
        }
    }

    private fun setupRecyclerView() {
        val adapterObj =
            PostAdapter(
                onLikeClicked = { postId -> viewModel.toggleLikeOnPost(postId) },
                onDeleteClicked = { post ->
                    // Listen for dialog result
                    setFragmentResultListener(DeleteConfirmationDialogFragment.REQUEST_KEY) {
                        _,
                        bundle ->
                        val confirmed =
                            bundle.getBoolean(DeleteConfirmationDialogFragment.RESULT_KEY)
                        if (confirmed) {
                            viewModel.deletePost(post)
                        }
                    }
                    val action =
                        ForumFragmentDirections.actionGlobalDeleteConfirmationDialog(
                            title = getString(R.string.delete_post_title),
                            message = getString(R.string.delete_post_message),
                            positive = getString(R.string.action_delete),
                            negative = getString(R.string.action_cancel),
                        )
                    findNavController().navigate(action)
                },
                currentUser = viewModel.getCurrentUser(),
            )
        postAdapter = adapterObj

        val layoutManager = LinearLayoutManager(context)
        binding.rvPosts.apply {
            adapter = postAdapter
            this.layoutManager = layoutManager

            // Restore scroll position
            post {
                val pos = viewModel.getScrollPosition()
                if (pos > 0) layoutManager.scrollToPosition(pos)
            }

            // Save scroll position on scroll
            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                            val first = layoutManager.findFirstVisibleItemPosition()
                            if (first >= 0) viewModel.saveScrollPosition(first)
                        }
                    }
                }
            )
        }
    }

    private fun observeViewModel() {
        viewModel.posts.observe(viewLifecycleOwner) { posts -> postAdapter?.submitList(posts) }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingIndicator.isVisible = isLoading == true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvPosts.adapter = null
        postAdapter = null
        _binding = null
    }
}
