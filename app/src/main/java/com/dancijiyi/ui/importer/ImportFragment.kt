package com.dancijiyi.ui.importer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.dancijiyi.R
import com.dancijiyi.data.AppContainer
import com.dancijiyi.databinding.FragmentImportBinding
import com.dancijiyi.ui.common.AppViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ImportFragment : Fragment(R.layout.fragment_import) {

    private var _binding: FragmentImportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ImportViewModel by viewModels {
        AppViewModelFactory(
            repository = AppContainer.wordRepository(requireContext()),
            appContext = requireContext().applicationContext,
        )
    }

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(viewModel::loadFile)
        }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openPicker()
            } else {
                showPermissionDeniedDialog()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImportBinding.bind(view)

        setupActions()
        observeState()
        observeEvents()
    }

    private fun setupActions() {
        binding.importPickButton.setOnClickListener { requestFileAccessAndPick() }
        binding.importButton.setOnClickListener { viewModel.importWords() }
    }

    private fun requestFileAccessAndPick() {
        // Android 13+ 通过 SAF 读取文档，不需要遗留存储权限。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            openPicker()
            return
        }

        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission,
            ) == PackageManager.PERMISSION_GRANTED -> openPicker()

            shouldShowRequestPermissionRationale(permission) -> showPermissionRationale()

            else -> permissionLauncher.launch(permission)
        }
    }

    private fun openPicker() {
        openDocumentLauncher.launch(
            arrayOf(
                "text/*",
                "application/csv",
                "application/vnd.ms-excel",
                "application/octet-stream",
            ),
        )
    }

    private fun showPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_permission_title)
            .setMessage(R.string.import_permission_rationale)
            .setPositiveButton(R.string.import_permission_continue) { _, _ ->
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_permission_title)
            .setMessage(R.string.import_permission_message)
            .setPositiveButton(R.string.import_permission_settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(R.string.import_permission_use_picker) { _, _ ->
                openPicker()
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null),
        )
        startActivity(intent)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun observeEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { event ->
                    when (event) {
                        is ImportEvent.Result -> showResultDialog(event)
                        is ImportEvent.Error -> showErrorDialog(event.message)
                    }
                }
            }
        }
    }

    private fun render(state: ImportUiState) {
        val busy = state.isBusy
        binding.root.findViewById<View>(R.id.loading_state).isVisible = busy
        binding.importContent.isVisible = !busy

        val preview = state.preview
        binding.importButton.isEnabled = !busy && preview?.entries?.isNotEmpty() == true

        binding.importFileName.text = state.sourceName?.let { name ->
            getString(R.string.import_file_selected, name)
        } ?: getString(R.string.import_file_none)

        if (preview == null) {
            binding.importPreviewSummary.setText(R.string.import_preview_empty)
            binding.importPreviewDetails.isVisible = false
            return
        }

        binding.importPreviewSummary.text = getString(
            R.string.import_preview_summary,
            preview.entries.size,
            preview.duplicateCount,
            preview.issues.size,
        )

        val details = preview.entries
            .take(5)
            .joinToString(separator = "\n") { "${it.word} → ${it.meaning}" }
        binding.importPreviewDetails.isVisible = true
        binding.importPreviewDetails.text = details.ifEmpty {
            getString(R.string.import_preview_no_valid)
        }
    }

    private fun showResultDialog(result: ImportEvent.Result) {
        val title = if (
            result.inserted > 0 ||
            (result.inserted == 0 && result.skipped > 0 && result.failed == 0)
        ) {
            R.string.import_result_title
        } else {
            R.string.import_result_failure_title
        }

        val message = buildString {
            append(
                getString(
                    R.string.import_result_message,
                    result.inserted,
                    result.skipped,
                    result.failed,
                ),
            )
            result.errorMessage?.let {
                append("\n\n")
                append(it)
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.action_ok, null)
            .show()
    }

    private fun showErrorDialog(message: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.import_result_failure_title)
            .setMessage(message)
            .setPositiveButton(R.string.action_ok, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
