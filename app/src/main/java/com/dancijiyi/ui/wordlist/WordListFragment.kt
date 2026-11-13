package com.dancijiyi.ui.wordlist

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dancijiyi.R
import com.dancijiyi.data.AppContainer
import com.dancijiyi.databinding.FragmentWordListBinding
import com.dancijiyi.ui.common.AppViewModelFactory
import com.dancijiyi.ui.common.WordEditorDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class WordListFragment : Fragment(R.layout.fragment_word_list) {

    private var _binding: FragmentWordListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordListViewModel by viewModels {
        AppViewModelFactory(
            repository = AppContainer.wordRepository(requireContext()),
            appContext = requireContext().applicationContext,
        )
    }

    private lateinit var wordAdapter: WordAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWordListBinding.bind(view)

        setupList()
        setupActions()
        setupEmptyState()
        observeUiState()
    }

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

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
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
        emptyState.findViewById<MaterialButton>(R.id.empty_secondary_button).isVisible = false
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
