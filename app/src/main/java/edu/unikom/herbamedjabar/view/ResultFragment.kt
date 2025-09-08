package edu.unikom.herbamedjabar.view

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import coil.load
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.databinding.FragmentResultBinding
import edu.unikom.herbamedjabar.util.MarkdownUtils
import edu.unikom.herbamedjabar.viewModel.ResultViewModel
import java.io.File

@AndroidEntryPoint
class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding
        get() = _binding!!

    private val viewModel: ResultViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ false)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, /* forward= */ true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListeners()
        observeViewModel()
    }

    private fun setupUI() {
        val args: ResultFragmentArgs by navArgs()
        val imagePath = args.imagePath
        val plantName = args.plantName
        val content = args.content
        val benefit = args.benefit
        val warning = args.warning
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
            benefitCard.isVisible = benefit.isNotBlank()
            warningCard.isVisible = warning.isNotBlank()
            primaryButton.isVisible = benefit.isNotBlank() || warning.isNotBlank()
            val imageFile = imagePath.let(::File)
            if (imageFile.exists()) {
                resultImageView.load(imageFile) {
                    placeholder(R.drawable.bg_place_holder)
                    error(R.drawable.bg_place_holder)
                    crossfade(true)
                }
            } else {
                resultImageView.setImageResource(R.drawable.bg_place_holder)
            }
            resultImageView.contentDescription =
                root.context.getString(R.string.cd_plant_image_of, plantName)
        }
    }

    private fun setupListeners() {
        val args: ResultFragmentArgs by navArgs()
        binding.topAppBar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.plantCardLayout.secondaryButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                "scan_again_request",
                Bundle().apply { putBoolean("open_camera", true) },
            )
            findNavController().navigateUp()
        }
        binding.plantCardLayout.primaryButton.setOnClickListener {
            val imagePath = args.imagePath
            val resultText = args.resultText.trim()
            val plantName = args.plantName.trim()

            val imageFile = imagePath.let(::File)
            if (!imageFile.exists() || resultText.isBlank() || plantName.isBlank()) {
                Snackbar.make(
                        requireView(),
                        getString(R.string.error_incomplete_post_data),
                        Snackbar.LENGTH_SHORT,
                    )
                    .show()
                return@setOnClickListener
            }

            val imageUri = Uri.fromFile(imageFile)

            viewModel.createPostFromScan(
                imageUri = imageUri,
                plantName = plantName,
                description = resultText,
            )
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingIndicator.isVisible = isLoading
            binding.plantCardLayout.primaryButton.isEnabled = !isLoading // Post button
            binding.plantCardLayout.secondaryButton.isEnabled = !isLoading // Scan again button
        }

        viewModel.postResult.observe(viewLifecycleOwner) { result ->
            result
                .onSuccess {
                    Snackbar.make(
                            requireView(),
                            getString(R.string.post_success),
                            Snackbar.LENGTH_SHORT,
                        )
                        .show()
                    // Navigate directly to ForumFragment
                    findNavController().popBackStack(R.id.forumFragment, false)
                }
                .onFailure {
                    Snackbar.make(
                            requireView(),
                            getString(
                                R.string.post_failed_with_reason,
                                it.message ?: getString(R.string.unknown_error),
                            ),
                            Snackbar.LENGTH_LONG,
                        )
                        .show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear transitions to prevent issues when navigating to top-level fragments
        enterTransition = null
        returnTransition = null
        reenterTransition = null
        exitTransition = null
        _binding = null
    }
}
