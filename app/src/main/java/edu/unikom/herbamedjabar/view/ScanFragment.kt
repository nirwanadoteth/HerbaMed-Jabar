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
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import edu.unikom.herbamedjabar.R
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
                    if (!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                        // Permanently denied: guide user to Settings
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.camera_permission_denied_permanent),
                            Toast.LENGTH_LONG
                        ).show()
                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            android.net.Uri.fromParts("package", requireContext().packageName, null)
                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                    } else {
                        // Temporarily denied: just inform
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.camera_permission_denied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    // Camera image capture launcher
    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val b = _binding ?: return@registerForActivityResult
                b.plantImageView.setImageBitmap(bitmap)
                b.plantImageView.contentDescription = getString(R.string.cd_plant_image)
                viewModel.analyzeImage(bitmap)
            }
        }

    // Gallery image picker launcher
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                lifecycleScope.launch {
                    val ctx = context ?: return@launch
                    val b = _binding ?: return@launch
                    val resolver = ctx.contentResolver
                    try {
                        val bitmap: Bitmap =
                            withContext(kotlinx.coroutines.Dispatchers.IO) {
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                                    // Pre-P: 2-pass decode with sampling
                                    val targetMaxDim = 2048
                                    val bounds =
                                        BitmapFactory.Options().apply { inJustDecodeBounds = true }
                                    resolver.openInputStream(uri)?.use { input ->
                                        BitmapFactory.decodeStream(input, null, bounds)
                                    }
                                        ?: throw IllegalStateException(
                                            "Gagal membuka stream (bounds)."
                                        )
                                    val maxDim =
                                        maxOf(bounds.outWidth, bounds.outHeight).takeIf { it > 0 }
                                            ?: targetMaxDim
                                    val sample = maxOf(1, maxDim / targetMaxDim)
                                    val opts =
                                        BitmapFactory.Options().apply { inSampleSize = sample }
                                    val decoded = resolver.openInputStream(uri)?.use { input ->
                                        BitmapFactory.decodeStream(input, null, opts)
                                    }
                                        ?: throw IllegalStateException("Gagal membuka stream (decode).")
                                    applyExifRotation(resolver, uri, decoded)
                                } else {
                                    val source = ImageDecoder.createSource(resolver, uri)
                                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                                        val maxDim = 2048
                                        val w = info.size.width
                                        val h = info.size.height
                                        val scale = maxOf(1, maxOf(w, h) / maxDim)
                                        decoder.setTargetSize(w / scale, h / scale)
                                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                    }
                                }
                            }
                        b.plantImageView.setImageBitmap(bitmap)
                        b.plantImageView.contentDescription = getString(R.string.cd_plant_image)
                        viewModel.analyzeImage(bitmap)
                    } catch (_: Exception) {
                        Toast.makeText(
                            ctx,
                            getString(R.string.error_image_load_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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

        parentFragmentManager.setFragmentResultListener("scan_again_request", this) { _, bundle ->
            if (bundle.getBoolean("open_camera")) {
                checkCameraPermissionAndOpenCamera()
            }
        }
        val pickVisualMediaRequest =
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        binding.scanButton.setOnClickListener { checkCameraPermissionAndOpenCamera() }
        binding.btnGallery.setOnClickListener { galleryLauncher.launch(pickVisualMediaRequest) }
    }

    private fun observeViewModel() {
        // Observe navigations
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
                    processingDialog = null
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

    private fun applyExifRotation(
        resolver: android.content.ContentResolver,
        uri: android.net.Uri,
        bmp: Bitmap
    ): Bitmap {
        return try {
            resolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                when (exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> bmp.rotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> bmp.rotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> bmp.rotate(270f)
                    else -> bmp
                }
            } ?: bmp
        } catch (_: Exception) {
            bmp
        }
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap =
        android.graphics.Matrix().let { m ->
            m.postRotate(degrees)
            Bitmap.createBitmap(this, 0, 0, width, height, m, true)
        }

    private fun checkCameraPermissionAndOpenCamera() {
        when {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> {
                takePictureLauncher.launch(null)
            }
            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                Toast.makeText(
                        requireContext(),
                        getString(R.string.camera_permission_rationale),
                        Toast.LENGTH_LONG,
                    )
                    .show()
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}
