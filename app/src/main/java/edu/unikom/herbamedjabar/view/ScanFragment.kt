package edu.unikom.herbamedjabar.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.databinding.FragmentScanBinding
import edu.unikom.herbamedjabar.viewModel.ScanViewModel
import edu.unikom.herbamedjabar.viewModel.UiState
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ScanFragment : Fragment() {

    private val viewModel: ScanViewModel by viewModels()
    private var _binding: FragmentScanBinding? = null
    private val binding
        get() = _binding!!

    private var processingDialog: ProcessingDialogFragment? = null

    // Camera permission launcher
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean
            ->
            if (isAdded) {
                if (isGranted) {
                    takePictureLauncher.launch(null)
                } else {
                    Toast.makeText(requireContext(), "Izin kamera ditolak", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    // Camera image capture launcher
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                binding.plantImageView.setImageBitmap(bitmap)
                viewModel.analyzeImage(bitmap)
            }
        }

    // Gallery image picker launcher
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try {
                    viewLifecycleOwner.lifecycleScope.launch {
                        var bounds: BitmapFactory.Options
                        val bitmap: Bitmap =
                            withContext(kotlinx.coroutines.Dispatchers.IO) {
                                (if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                                    // Pre-P: decode with sampling
                                    val resolver = requireContext().contentResolver
                                    resolver.openInputStream(uri)!!.use { input ->
                                        // bounds pass
                                        bounds =
                                            BitmapFactory.Options().apply {
                                                inJustDecodeBounds = true
                                            }
                                        BitmapFactory.decodeStream(
                                            input,
                                            null,
                                            bounds,
                                        )
                                    }
                                    val targetMaxDim = 2048
                                    val resolver2 = requireContext().contentResolver
                                    resolver2.openInputStream(uri)!!.use { input ->
                                        val opts =
                                            BitmapFactory.Options().apply {
                                                inSampleSize =
                                                    maxOf(
                                                        1,
                                                        maxOf(bounds.outWidth, bounds.outHeight) /
                                                                targetMaxDim,
                                                    )
                                            }
                                        BitmapFactory.decodeStream(
                                            input,
                                            null,
                                            opts,
                                        )
                                    }
                                } else {
                                    val source =
                                        ImageDecoder.createSource(
                                            requireContext().contentResolver,
                                            uri,
                                        )
                                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                        val maxDim = 2048
                                        val w = info.size.width
                                        val h = info.size.height
                                        val scale = maxOf(1, maxOf(w, h) / maxDim)
                                        decoder.setTargetSize(w / scale, h / scale)
                                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                    }
                                })!!
                            }
                        binding.plantImageView.setImageBitmap(bitmap)
                        viewModel.analyzeImage(bitmap)
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()

        val fullText = getString(edu.unikom.herbamedjabar.R.string.herbamed)
        val medStart = fullText.indexOf("Med")
        val medEnd = if (medStart >= 0) medStart + MED_SUBSTRING_LENGTH else medStart
        val spannable = android.text.SpannableString(fullText)
        if (medStart >= 0) {
            val primaryColor =
                ContextCompat.getColor(
                    requireContext(),
                    edu.unikom.herbamedjabar.R.color.md_theme_primary,
                )
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(primaryColor),
                medStart,
                medEnd,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
        binding.appTitleTextView.text = spannable

        parentFragmentManager.setFragmentResultListener("scan_again_request", this) { _, bundle ->
            if (bundle.getBoolean("open_camera")) {
                checkCameraPermissionAndOpenCamera()
            }
        }

        binding.scanButton.setOnClickListener { checkCameraPermissionAndOpenCamera() }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
    }

    private fun observeViewModel() {
        // Observe navigation
        viewModel.navigateToResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                (activity as? MainActivity)?.showResultFragment(it)
                viewModel.onNavigationComplete()
            }
        }

        // Observe UI state (loading, error, etc)
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is UiState.Loading -> {
                    if (processingDialog == null || processingDialog?.dialog?.isShowing == false) {
                        processingDialog = ProcessingDialogFragment()
                        processingDialog?.show(childFragmentManager, ProcessingDialogFragment.TAG)
                    }
                }

                is UiState.Success,
                is UiState.Error -> {
                    processingDialog?.dismiss()
                    if (state is UiState.Error) {
                        context?.let { ctx ->
                            Toast.makeText(ctx, state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }

                else -> {
                    /* Idle */
                }
            }
        }

        // Observe scanStats and update TextViews
        viewModel.scanStats.observe(viewLifecycleOwner) { stats ->
            binding.totalScanTextView.text = stats.total.toString()
            binding.herbalScanTextView.text = stats.herbal.toString()
            binding.nonHerbalScanTextView.text = stats.nonHerbal.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        processingDialog?.dismiss()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                takePictureLauncher.launch(null)
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    companion object {
        private const val MED_SUBSTRING_LENGTH = 3
    }
}
