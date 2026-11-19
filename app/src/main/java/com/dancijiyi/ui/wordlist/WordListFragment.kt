package com.dancijiyi.ui.wordlist

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dancijiyi.R
import com.dancijiyi.data.AppContainer
import com.dancijiyi.data.exporter.ExportFormat
import com.dancijiyi.databinding.FragmentWordListBinding
import com.dancijiyi.ui.common.AppViewModelFactory
import com.dancijiyi.ui.common.WordEditorDialog
import com.dancijiyi.ui.exporter.ExportEvent
import com.dancijiyi.ui.exporter.ExportUiState
import com.dancijiyi.ui.exporter.ExportViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class WordListFragment : Fragment(R.layout.fragment_word_list) {

    private var _binding: FragmentWordListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordListViewModel by viewModels { createFactory() }
    private val exportViewModel: ExportViewModel by viewModels { createFactory() }

    private lateinit var wordAdapter: WordAdapter
    private var pendingExportFormat: ExportFormat? = null

    private val exportPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val format = pendingExportFormat
            pendingExportFormat = null
            if (granted && format != null) {
                exportViewModel.export(format)
            } else {
                showExportPermissionDeniedDialog()
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWordListBinding.bind(view)

        setupList()
        setupActions()
        setupEmptyState()
        observeUiState()
        observeExport()
    }

    private fun createFactory(): AppViewModelFactory = AppViewModelFactory(
        repository = AppContainer.wordRepository(requireContext()),
        appContext = requireContext().applicationContext,
    )

    private fun setupList() {
        wordAdapter = WordAdapter { word ->
            findNavController().navigate(
                R.id.action_wordList_to_wordDetail,
                Bundle().apply { putLong(ARG_WORD_ID, word.id) },
            )
        }
        binding.wordRecycler.adapter = wordAdapter
    }

    private fun setupActions() {
        binding.addWordFab.setOnClickListener { showAddDialog() }
        binding.exportButton.setOnClickListener { showExportFormatDialog() }

        binding.filterChips.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_due -> viewModel.setFilter(WordFilter.DUE)
                R.id.chip_mastered -> viewModel.setFilter(WordFilter.MASTERED)
                R.id.chip_all -> viewModel.setFilter(WordFilter.ALL)
            }
        }
    }

    private fun setupEmptyState() {
        val emptyState = binding.root.findViewById<View>(R.id.empty_state)
        emptyState.findViewById<TextView>(R.id.empty_title).setText(R.string.empty_list_title)
        emptyState.findViewById<TextView>(R.id.empty_message).setText(R.string.empty_list_message)

        emptyState.findViewById<MaterialButton>(R.id.empty_primary_button).apply {
            setText(R.string.action_add_word)
            setOnClickListener { showAddDialog() }
        }
        emptyState.findViewById<MaterialButton>(R.id.empty_secondary_button).isVisible = false
    }

    private fun showAddDialog() {
        WordEditorDialog.show(requireContext(), layoutInflater) { word, meaning ->
            viewModel.addWord(word, meaning)
        }
    }

    private fun showExportFormatDialog() {
        val formats = arrayOf(
            getString(R.string.export_format_csv),
            getString(R.string.export_format_txt),
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.export_format_title)
            .setItems(formats) { _, which ->
                startExport(if (which == 0) ExportFormat.CSV else ExportFormat.TXT)
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun startExport(format: ExportFormat) {
        // Android 10+ 使用 MediaStore，无需存储权限；旧版本才检查/申请写入权限。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportViewModel.export(format)
            return
        }

        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission,
            ) == PackageManager.PERMISSION_GRANTED -> exportViewModel.export(format)

            shouldShowRequestPermissionRationale(permission) -> {
                pendingExportFormat = format
                showExportPermissionRationale()
            }

            else -> {
                pendingExportFormat = format
                exportPermissionLauncher.launch(permission)
            }
        }
    }

    private fun showExportPermissionRationale() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.export_permission_title)
            .setMessage(R.string.export_permission_rationale)
            .setPositiveButton(R.string.export_permission_continue) { _, _ ->
                pendingExportFormat?.let {
                    exportPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
            .setNegativeButton(R.string.action_cancel) { _, _ ->
                pendingExportFormat = null
            }
            .show()
    }

    private fun showExportPermissionDeniedDialog() {
        pendingExportFormat = null
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.export_permission_title)
            .setMessage(R.string.export_permission_message)
            .setPositiveButton(R.string.export_permission_settings) { _, _ ->
                openAppSettings()
            }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun openAppSettings() {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", requireContext().packageName, null),
            ),
        )
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun observeExport() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { exportViewModel.uiState.collect(::renderExportState) }
                launch { exportViewModel.events.collect(::handleExportEvent) }
            }
        }
    }

    private fun renderExportState(state: ExportUiState) {
        binding.exportButton.isEnabled = !state.isExporting
        binding.exportButton.setText(
            if (state.isExporting) R.string.export_in_progress else R.string.export_action,
        )
    }

    private fun handleExportEvent(event: ExportEvent) {
        when (event) {
            is ExportEvent.Success -> MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.export_success_title)
                .setMessage(getString(R.string.export_success_message, event.path))
                .setPositiveButton(R.string.action_ok, null)
                .show()

            is ExportEvent.Error -> MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.export_failure_title)
                .setMessage(event.message)
                .setPositiveButton(R.string.action_ok, null)
                .show()
        }
    }

    private fun render(state: WordListUiState) {
        val showEmpty = !state.isLoading && state.words.isEmpty()
        binding.root.findViewById<View>(R.id.loading_state).isVisible = state.isLoading
        binding.root.findViewById<View>(R.id.empty_state).isVisible = showEmpty
        binding.listContent.isVisible = !state.isLoading && state.words.isNotEmpty()
        binding.addWordFab.isVisible = !state.isLoading && state.words.isNotEmpty()

        syncFilterChip(state.filter)
        wordAdapter.submitList(state.words)
        binding.listSummary.text = getString(
            R.string.list_summary_format,
            state.totalCount,
            state.dueCount,
        )

        if (showEmpty) {
            renderEmptyState(state.totalCount > 0)
        }

        state.errorMessage?.let { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            viewModel.consumeError()
        }
    }

    private fun renderEmptyState(filteredEmpty: Boolean) {
        val emptyState = binding.root.findViewById<View>(R.id.empty_state)
        emptyState.findViewById<TextView>(R.id.empty_title).setText(
            if (filteredEmpty) R.string.empty_filter_title else R.string.empty_list_title,
        )
        emptyState.findViewById<TextView>(R.id.empty_message).setText(
            if (filteredEmpty) R.string.empty_filter_message else R.string.empty_list_message,
        )

        val primaryButton = emptyState.findViewById<MaterialButton>(R.id.empty_primary_button)
        primaryButton.isVisible = true
        if (filteredEmpty) {
            primaryButton.setText(R.string.action_view_all)
            primaryButton.setOnClickListener { viewModel.setFilter(WordFilter.ALL) }
        } else {
            primaryButton.setText(R.string.action_add_word)
            primaryButton.setOnClickListener { showAddDialog() }
        }

        val secondaryButton = emptyState.findViewById<MaterialButton>(R.id.empty_secondary_button)
        if (filteredEmpty) {
            secondaryButton.isVisible = true
            secondaryButton.setText(R.string.export_action)
            secondaryButton.setOnClickListener { showExportFormatDialog() }
        } else {
            secondaryButton.isVisible = false
        }
    }

    private fun syncFilterChip(filter: WordFilter) {
        val chipId = when (filter) {
            WordFilter.ALL -> R.id.chip_all
            WordFilter.DUE -> R.id.chip_due
            WordFilter.MASTERED -> R.id.chip_mastered
        }
        if (binding.filterChips.checkedChipId != chipId) {
            binding.filterChips.check(chipId)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_WORD_ID = "wordId"
    }
}
