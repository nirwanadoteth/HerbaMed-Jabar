package edu.unikom.herbamedjabar.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.adapter.HistoryAdapter
import edu.unikom.herbamedjabar.databinding.FragmentHistoryBinding
import edu.unikom.herbamedjabar.viewModel.HistoryViewModel

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels()
    private var historyAdapter: HistoryAdapter? = null

    private var _binding: FragmentHistoryBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { history ->
            val directions =
                HistoryFragmentDirections.actionHistoryFragmentToHistoryDetailFragment(history)
            findNavController().navigate(directions)
        }
        val layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.historyRecyclerView.adapter = historyAdapter
        binding.historyRecyclerView.layoutManager = layoutManager
        binding.historyRecyclerView.setHasFixedSize(true)

        // Restore scroll position
        binding.historyRecyclerView.post {
            val pos = viewModel.getScrollPosition()
            if (pos > 0) layoutManager.scrollToPosition(pos)
        }

        // Save scroll position on scroll
        binding.historyRecyclerView.addOnScrollListener(
            object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    newState: Int,
                ) {
                    if (newState == androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE) {
                        val first = layoutManager.findFirstVisibleItemPosition()
                        if (first >= 0) viewModel.saveScrollPosition(first)
                    }
                }
            }
        )
    }

    private fun observeViewModel() {
        viewModel.allHistory.observe(viewLifecycleOwner) { historyList ->
            val isEmpty = historyList.isEmpty()
            binding.historyRecyclerView.isVisible = !isEmpty
            binding.emptyHistoryImageView.isVisible = isEmpty
            historyAdapter?.submitList(historyList)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.historyRecyclerView.adapter = null
        historyAdapter = null
        _binding = null
    }
}
