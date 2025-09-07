package edu.unikom.herbamedjabar.view

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.color.MaterialColors
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

        val args: HistoryDetailFragmentArgs by navArgs()
        val history = args.history
        setupView(history)
        setupAction(history)
    }

    private fun setupView(history: ScanHistory) {
        val historyId = history.id
        val imagePath = history.imagePath
        val plantName = history.plantName
        val content = history.content
        val benefit = history.benefit
        val warning = history.warning
        val card = binding.plantCardLayout

        card.apply {
            plantNameTextView.text = plantName

            val key = "history:${historyId}:content:${content.hashCode()}"
            val benefitKey = "history:${historyId}:benefit:${benefit.hashCode()}"
            val warningKey = "history:${historyId}:warning:${warning.hashCode()}"

            val contentSpanned = MarkdownUtils.parseMarkdownToSpanned(content, key)
            val benefitSpanned = MarkdownUtils.parseMarkdownToSpanned(benefit, benefitKey, true)
            val warningSpanned = MarkdownUtils.parseMarkdownToSpanned(warning, warningKey, true)

            contentTextView.text = contentSpanned
            benefitTextView.text = benefitSpanned
            warningTextView.text = warningSpanned
            benefitCard.isVisible = benefit.isNotBlank()
            warningCard.isVisible = warning.isNotBlank()
            resultImageView.load(imagePath.let(::File)) {
                placeholder(R.drawable.bg_place_holder)
                error(R.drawable.bg_place_holder)
                fallback(R.drawable.bg_place_holder)
                crossfade(true)
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
            icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
            val errorContainer =
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorErrorContainer,
                )
            val onErrorContainer =
                MaterialColors.getColor(
                    this,
                    com.google.android.material.R.attr.colorOnErrorContainer,
                )
            iconTint = ColorStateList.valueOf(onErrorContainer)
            backgroundTintList = ColorStateList.valueOf(errorContainer)
            setTextColor(onErrorContainer)

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

    // SafeArgs handles argument passing; no need for companion object
}
