package edu.unikom.herbamedjabar.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.adapter.HistoryAdapter
import edu.unikom.herbamedjabar.databinding.FragmentHistoryBinding
import edu.unikom.herbamedjabar.viewModel.HistoryViewModel

@AndroidEntryPoint
class HistoryFragment : Fragment() {

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var historyAdapter: HistoryAdapter

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter {
            (activity as? MainActivity)?.showHistoryDetailFragment(it)
        }
        binding.historyRecyclerView.adapter = historyAdapter
        binding.historyRecyclerView.setHasFixedSize(true)
    }

    private fun observeViewModel() {
        viewModel.allHistory.observe(viewLifecycleOwner) { historyList ->
            if (historyList.isEmpty()) {
                binding.historyRecyclerView.visibility = View.GONE
                binding.emptyHistoryImageView.visibility = View.VISIBLE
            } else {
                binding.historyRecyclerView.visibility = View.VISIBLE
                binding.emptyHistoryImageView.visibility = View.GONE
                historyAdapter.submitList(historyList)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
