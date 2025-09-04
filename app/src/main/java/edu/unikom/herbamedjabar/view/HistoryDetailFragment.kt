package edu.unikom.herbamedjabar.view

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil.load
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.data.ScanHistory
import edu.unikom.herbamedjabar.databinding.FragmentHistoryDetailBinding
import edu.unikom.herbamedjabar.util.MarkdownUtils
import edu.unikom.herbamedjabar.viewModel.HistoryDetailViewModel
import java.io.File

@AndroidEntryPoint
class HistoryDetailFragment : Fragment() {

    private val viewModel: HistoryDetailViewModel by viewModels()
    private var _binding: FragmentHistoryDetailBinding? = null
    private val binding
        get() = _binding!!

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

        val history =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                @Suppress("DEPRECATION") arguments?.getParcelable(EXTRA_HISTORY)
            } else {
                arguments?.getParcelable(EXTRA_HISTORY, ScanHistory::class.java)
            }

        if (history != null) {
            setupView()
            setupAction(history)
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupView() {
        val imagePath = arguments?.getString(ARG_IMAGE_PATH)
        val plantName = arguments?.getString(ARG_PLANT_NAME).orEmpty()
        val content = arguments?.getString(ARG_CONTENT).orEmpty()
        val benefit = arguments?.getString(ARG_BENEFIT).orEmpty()
        val warning = arguments?.getString(ARG_WARNING).orEmpty()
        val card = binding.plantCardLayout

        card.apply {
            plantNameTextView.text = plantName
            contentTextView.text =
                HtmlCompat.fromHtml(
                    MarkdownUtils.parseMarkdownToHtml(content),
                    HtmlCompat.FROM_HTML_MODE_COMPACT,
                )
            benefitTextView.text =
                HtmlCompat.fromHtml(
                    MarkdownUtils.parseMarkdownToHtml(benefit),
                    HtmlCompat.FROM_HTML_MODE_COMPACT,
                )
            warningTextView.text =
                HtmlCompat.fromHtml(
                    MarkdownUtils.parseMarkdownToHtml(warning),
                    HtmlCompat.FROM_HTML_MODE_COMPACT,
                )
            benefitCard.visibility = if (benefit.isBlank()) View.GONE else View.VISIBLE
            warningCard.visibility = if (warning.isBlank()) View.GONE else View.VISIBLE
            if (imagePath != null) {
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    resultImageView.load(Uri.fromFile(imageFile))
                } else {
                    resultImageView.setImageResource(R.drawable.bg_place_holder)
                }
            } else {
                resultImageView.setImageResource(R.drawable.bg_place_holder)
            }
            resultImageView.contentDescription =
                root.context.getString(R.string.cd_plant_image_of, plantName)
        }
    }

    private fun setupAction(history: ScanHistory) {
        binding.topAppBar.setNavigationOnClickListener { parentFragmentManager.popBackStack() }
        val deleteButton = binding.plantCardLayout.secondaryButton
        deleteButton.apply {
            text = getString(R.string.action_delete)
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash)
            iconTint =
                ContextCompat.getColorStateList(requireContext(), R.color.md_theme_onErrorContainer)
            setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.md_theme_errorContainer)
            )
            setTextColor(
                ContextCompat.getColor(requireContext(), R.color.md_theme_onErrorContainer)
            )
            setOnClickListener {
                viewModel.deleteHistory(history)
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val EXTRA_HISTORY: String = "extra_history"
        private const val ARG_IMAGE_PATH = "image_path"
        private const val ARG_RESULT_TEXT = "result_text"
        private const val ARG_PLANT_NAME = "plant_name"
        private const val ARG_BENEFIT = "benefit"
        private const val ARG_WARNING = "warning"
        private const val ARG_CONTENT = "content"

        fun newInstance(history: ScanHistory): HistoryDetailFragment {
            val fragment = HistoryDetailFragment()
            val bundle =
                Bundle().apply {
                    putParcelable(EXTRA_HISTORY, history)
                    putString(ARG_IMAGE_PATH, history.imagePath)
                    putString(ARG_RESULT_TEXT, history.resultText)
                    putString(ARG_PLANT_NAME, history.plantName)
                    putString(ARG_BENEFIT, history.benefit)
                    putString(ARG_WARNING, history.warning)
                    putString(ARG_CONTENT, history.content)
                }
            fragment.arguments = bundle
            return fragment
        }
    }
}
