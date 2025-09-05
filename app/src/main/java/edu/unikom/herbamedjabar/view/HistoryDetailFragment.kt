package edu.unikom.herbamedjabar.view

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        val history = arguments?.let {
            androidx.core.os.BundleCompat.getParcelable(it, EXTRA_HISTORY, ScanHistory::class.java)
        }

        if (history != null) {
            setupView(history)
            setupAction(history)
        } else {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupView(history: ScanHistory) {
        val historyId = history.id
        val imagePath = arguments?.getString(ARG_IMAGE_PATH)
        val plantName = arguments?.getString(ARG_PLANT_NAME).orEmpty()
        val content = arguments?.getString(ARG_CONTENT).orEmpty()
        val benefit = arguments?.getString(ARG_BENEFIT).orEmpty()
        val warning = arguments?.getString(ARG_WARNING).orEmpty()
        val card = binding.plantCardLayout

        card.apply {
            plantNameTextView.text = plantName

            val key = "history:content:${historyId}"
            val benefitKey = "history:benefit:${historyId}"
            val warningKey = "history:warning:${historyId}"

            val contentSpanned = MarkdownUtils.parseMarkdownToSpanned(content, key)
            val benefitSpanned = MarkdownUtils.parseMarkdownToSpanned(benefit, benefitKey, true)
            val warningSpanned = MarkdownUtils.parseMarkdownToSpanned(warning, warningKey, true)

            contentTextView.text = contentSpanned
            benefitTextView.text = benefitSpanned
            warningTextView.text = warningSpanned
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
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.delete_history_title)
                    .setMessage(R.string.delete_history_message)
                    .setPositiveButton(R.string.action_delete) { _, _ ->
                        viewModel.deleteHistory(history)
                        parentFragmentManager.popBackStack()
                    }
                    .setNegativeButton(R.string.action_cancel, null)
                    .show()
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
