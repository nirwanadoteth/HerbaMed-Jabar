package edu.unikom.herbamedjabar.view

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.databinding.FragmentResultBinding
import edu.unikom.herbamedjabar.viewModel.ResultViewModel
import java.io.File
import edu.unikom.herbamedjabar.util.MarkdownUtils

@AndroidEntryPoint
class ResultFragment : Fragment() {

    private var _binding: FragmentResultBinding? = null
    private val binding get() = _binding!!

    // Inject ResultViewModel
    private val viewModel: ResultViewModel by viewModels()

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
        val imagePath = arguments?.getString(ARG_IMAGE_PATH)
        val plantName = arguments?.getString(ARG_PLANT_NAME).orEmpty()
        val content = arguments?.getString(ARG_CONTENT).orEmpty()
        val benefit = arguments?.getString(ARG_BENEFIT).orEmpty()
        val warning = arguments?.getString(ARG_WARNING).orEmpty()

        binding.apply {
            plantNameTextView.text = plantName
            contentTextView.text = HtmlCompat.fromHtml(
                MarkdownUtils.parseMarkdownToHtml(content),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            benefitTextView.text = HtmlCompat.fromHtml(
                MarkdownUtils.parseMarkdownToHtml(benefit),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            warningTextView.text = HtmlCompat.fromHtml(
                MarkdownUtils.parseMarkdownToHtml(warning),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            if (benefit.isBlank()) {
                benefitBanner.visibility = View.GONE
                benefitTextView.visibility = View.GONE
            }
            if (warning.isBlank()) {
                warningBanner.visibility = View.GONE
                warningTextView.visibility = View.GONE
            }
            if (imagePath != null) {
                val imageFile = File(imagePath)
                if (imageFile.exists()) {
                    resultImageView.setImageURI(Uri.fromFile(imageFile))
                }
            }
        }
    }

    private fun setupListeners() {
        binding.backButton.setOnClickListener { activity?.supportFragmentManager?.popBackStack() }

        binding.scanAgainButton.setOnClickListener {
            parentFragmentManager.setFragmentResult(
                "scan_again_request",
                Bundle().apply { putBoolean("open_camera", true) },
            )
            activity?.supportFragmentManager?.popBackStack()
        }

        binding.btnPostToForum.setOnClickListener {
            val imagePath = arguments?.getString(ARG_IMAGE_PATH)
            val resultText = arguments?.getString(ARG_RESULT_TEXT)

            if (imagePath == null || resultText == null) {
                Toast.makeText(
                    requireContext(),
                    "Data tidak lengkap untuk diposting",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val plantName = resultText.lines().firstOrNull()
                ?.replace("#", "")?.replace("*", "")?.trim() ?: "Tanaman Hasil Scan"

            val imageUri = Uri.fromFile(File(imagePath))

            viewModel.createPostFromScan(
                imageUri = imageUri,
                plantName = plantName,
                description = resultText
            )
        }
    }

    private fun observeViewModel() {
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnPostToForum.isEnabled = !isLoading
            binding.scanAgainButton.isEnabled = !isLoading
        }

        viewModel.postResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                Toast.makeText(requireContext(), "Berhasil diposting ke forum!", Toast.LENGTH_SHORT)
                    .show()
                // Kembali ke halaman scan setelah berhasil
                activity?.supportFragmentManager?.popBackStack()
            }.onFailure {
                Toast.makeText(
                    requireContext(),
                    "Gagal memposting: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_IMAGE_PATH = "image_path"
        private const val ARG_RESULT_TEXT = "result_text"
        private const val ARG_PLANT_NAME = "plant_name"
        private const val ARG_BENEFIT = "benefit"
        private const val ARG_WARNING = "warning"
        private const val ARG_CONTENT = "content"

        fun newInstance(
            imagePath: String,
            resultText: String,
            plantName: String,
            benefit: String,
            warning: String,
            content: String
        ): ResultFragment {
            val fragment = ResultFragment()
            val args = Bundle().apply {
                putString(ARG_IMAGE_PATH, imagePath)
                putString(ARG_RESULT_TEXT, resultText)
                putString(ARG_PLANT_NAME, plantName)
                putString(ARG_BENEFIT, benefit)
                putString(ARG_WARNING, warning)
                putString(ARG_CONTENT, content)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
