package edu.unikom.herbamedjabar.view

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.data.ScanHistory
import edu.unikom.herbamedjabar.databinding.FragmentHistoryDetailBinding
import edu.unikom.herbamedjabar.viewModel.HistoryDetailViewModel
import edu.unikom.herbamedjabar.util.MarkdownUtils
import java.io.File

@AndroidEntryPoint
class HistoryDetailFragment : Fragment() {

    private val viewModel: HistoryDetailViewModel by viewModels()
    private var _binding: FragmentHistoryDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val history = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            @Suppress("DEPRECATION")
            arguments?.getParcelable(EXTRA_HISTORY)
        } else {
            arguments?.getParcelable(EXTRA_HISTORY, ScanHistory::class.java)
        }

        if (history != null) {
            setupView(history)
            setupAction(history)
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupView(history: ScanHistory) {
        val imageFile = File(history.imagePath)

        binding.apply {
            plantNameTextView.text = history.plantName
            contentTextView.text = HtmlCompat.fromHtml(MarkdownUtils.parseMarkdownToHtml(history.content), HtmlCompat.FROM_HTML_MODE_LEGACY)
            benefitTextView.text = HtmlCompat.fromHtml(MarkdownUtils.parseMarkdownToHtml(history.benefit), HtmlCompat.FROM_HTML_MODE_LEGACY)
            warningTextView.text = HtmlCompat.fromHtml(MarkdownUtils.parseMarkdownToHtml(history.warning), HtmlCompat.FROM_HTML_MODE_LEGACY)
            if (history.benefit.isBlank()) {
                benefitBanner.visibility = View.GONE
                benefitTextView.visibility = View.GONE
            }
            if (history.warning.isBlank()) {
                warningBanner.visibility = View.GONE
                warningTextView.visibility = View.GONE
            }
            if (imageFile.exists()) {
                resultImageView.load(Uri.fromFile(imageFile)) { crossfade(true) }
            }
        }
    }

    private fun setupAction(history: ScanHistory) {
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.deleteButton.setOnClickListener {
            viewModel.deleteHistory(history)
            parentFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val EXTRA_HISTORY: String = "extra_history"

        fun newInstance(history: ScanHistory): HistoryDetailFragment =
            HistoryDetailFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_HISTORY, history)
                }
            }
    }
}
