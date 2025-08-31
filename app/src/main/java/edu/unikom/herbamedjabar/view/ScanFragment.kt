package edu.unikom.herbamedjabar.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.databinding.FragmentScanBinding
import edu.unikom.herbamedjabar.viewModel.ScanViewModel
import edu.unikom.herbamedjabar.viewModel.UiState

@AndroidEntryPoint
class ScanFragment : Fragment() {

    private val viewModel: ScanViewModel by viewModels()
    private var _binding: FragmentScanBinding? = null
    private val binding
        get() = _binding!!

    private var processingDialog: ProcessingDialogFragment? = null

    // Launcher untuk izin kamera
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isAdded) {
                if (isGranted) {
                    takePictureLauncher.launch(null)
                } else {
                    Toast.makeText(requireContext(), "Izin kamera ditolak", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    // Launcher untuk mengambil gambar dari kamera
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                binding.plantImageView.setImageBitmap(bitmap)
                viewModel.analyzeImage(bitmap)
            }
        }

    // Launcher BARU untuk mengambil gambar dari galeri
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                        @Suppress("DEPRECATION")
                        MediaStore.Images.Media.getBitmap(requireActivity().contentResolver, uri)
                    } else {
                        val source = ImageDecoder.createSource(requireActivity().contentResolver, uri)
                        ImageDecoder.decodeBitmap(source)
                    }
                    binding.plantImageView.setImageBitmap(bitmap)
                    viewModel.analyzeImage(bitmap)
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

        // Set 'HerbaMed' with 'Med' in primary color
        val fullText = "HerbaMed"
        val spannable = android.text.SpannableString(fullText)
        val medStart = fullText.indexOf("Med")
        val medEnd = medStart + 3
        val primaryColor = ContextCompat.getColor(requireContext(), edu.unikom.herbamedjabar.R.color.primary)
        spannable.setSpan(
            android.text.style.ForegroundColorSpan(primaryColor),
            medStart, medEnd,
            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.appTitleTextView.text = spannable

        parentFragmentManager.setFragmentResultListener("scan_again_request", this) { _, bundle ->
            if (bundle.getBoolean("open_camera")) {
                checkCameraPermissionAndOpenCamera()
            }
        }

        // Hubungkan tombol dengan fungsinya masing-masing
        binding.scanButton.setOnClickListener { checkCameraPermissionAndOpenCamera() }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch("image/*") }
    }

    private fun observeViewModel() {
        // Observer untuk navigasi
        viewModel.navigateToResult.observe(viewLifecycleOwner) { result ->
            result?.let {
                // Panggil fungsi di MainActivity untuk menampilkan halaman hasil
                (activity as? MainActivity)?.showResultFragment(it.imagePath, it.resultText)
                viewModel.onNavigationComplete() // Reset state
            }
        }

        // Observer untuk UI State (loading, error, etc)
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
}
