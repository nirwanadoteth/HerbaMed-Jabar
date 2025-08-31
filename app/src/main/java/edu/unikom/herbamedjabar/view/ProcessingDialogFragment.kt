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
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set dialog agar fullscreen
        setStyle(STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProcessingDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            var progress = 0
            while (progress <= 100) {
                binding.progressBar.progress = progress
                binding.progressTextView.text = getString(R.string.progress_percent, progress)
                progress++

                val randomDelay = (50..150).random().toLong()
                delay(randomDelay)
                if (progress > 95) {
                    delay(1000)
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
    }
}
