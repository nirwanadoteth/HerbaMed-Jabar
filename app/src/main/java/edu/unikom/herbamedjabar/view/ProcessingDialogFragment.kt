package edu.unikom.herbamedjabar.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.databinding.FragmentProcessingDialogBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ProcessingDialogFragment : DialogFragment() {

    private var _binding: FragmentProcessingDialogBinding? = null
    private val binding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set dialog agar fullscreen
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentProcessingDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            var progress = 0
            while (progress <= MAX_PROGRESS) {
                binding.progressBar.progress = progress
                binding.progressTextView.text = getString(R.string.progress_percent, progress)
                progress++

                val randomDelay = (MIN_DELAY_MS..MAX_DELAY_MS).random()
                delay(randomDelay)
                if (progress > FINAL_PROGRESS_THRESHOLD) {
                    delay(FINAL_DELAY_MS)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG: String = "ProcessingDialog"
        private const val MAX_PROGRESS = 100
        private const val MIN_DELAY_MS = 50L
        private const val MAX_DELAY_MS = 150L
        private const val FINAL_DELAY_MS = 1000L
        private const val FINAL_PROGRESS_THRESHOLD = 95
    }
}
