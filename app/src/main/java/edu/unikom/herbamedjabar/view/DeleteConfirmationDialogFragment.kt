package edu.unikom.herbamedjabar.view

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialElevationScale

class DeleteConfirmationDialogFragment : DialogFragment() {
    private val args: DeleteConfirmationDialogFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialElevationScale(true)
        returnTransition = MaterialElevationScale(false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(args.title)
            .setMessage(args.message)
            .setPositiveButton(args.positive) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply { putBoolean(RESULT_KEY, true) },
                )
                dismiss()
            }
            .setNegativeButton(args.negative) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply { putBoolean(RESULT_KEY, false) },
                )
                dismiss()
            }
            .create()
    }

    companion object {
        const val REQUEST_KEY = "delete_confirmation_request"
        const val RESULT_KEY = "delete_confirmed"
    }
}
