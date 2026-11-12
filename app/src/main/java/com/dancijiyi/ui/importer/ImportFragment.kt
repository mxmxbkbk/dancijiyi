package com.dancijiyi.ui.importer

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dancijiyi.R
import com.dancijiyi.databinding.FragmentImportBinding
import com.dancijiyi.ui.common.ScreenState
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ImportFragment : Fragment(R.layout.fragment_import) {

    private var _binding: FragmentImportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ImportViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImportBinding.bind(view)

        setupActions()
        setupEmptyState()
        observeUiState()
    }

    private fun setupActions() {
        binding.importButton.setOnClickListener {
            val rawText = binding.importInput.text?.toString().orEmpty()
            viewModel.importWords(rawText)
        }

        binding.importFileButton.setOnClickListener {
            Toast.makeText(requireContext(), R.string.import_file_todo, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupEmptyState() {
        val emptyState = binding.root.findViewById<View>(R.id.empty_state)
        emptyState.findViewById<TextView>(R.id.empty_title).setText(R.string.empty_import_title)
        emptyState.findViewById<TextView>(R.id.empty_message).setText(R.string.empty_import_message)

        emptyState.findViewById<MaterialButton>(R.id.empty_primary_button).apply {
            setText(R.string.action_back_to_list)
            setOnClickListener { findNavController().navigateUp() }
        }
        emptyState.findViewById<MaterialButton>(R.id.empty_secondary_button).isVisible = false
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ScreenState) {
        binding.root.findViewById<View>(R.id.loading_state).isVisible =
            state == ScreenState.LOADING
        binding.root.findViewById<View>(R.id.empty_state).isVisible =
            state == ScreenState.EMPTY
        binding.importContent.isVisible = state == ScreenState.CONTENT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
