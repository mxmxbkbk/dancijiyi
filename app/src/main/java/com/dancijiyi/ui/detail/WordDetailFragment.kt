package com.dancijiyi.ui.detail

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
import com.dancijiyi.data.Word
import com.dancijiyi.databinding.FragmentWordDetailBinding
import com.dancijiyi.ui.common.AppViewModelFactory
import com.dancijiyi.ui.common.WordEditorDialog
import com.dancijiyi.ui.common.formatTimestamp
import com.dancijiyi.ui.common.stageLabel
import com.dancijiyi.ui.wordlist.WordListFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class WordDetailFragment : Fragment(R.layout.fragment_word_detail) {

    private var _binding: FragmentWordDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordDetailViewModel by viewModels {
        AppViewModelFactory(AppContainer.wordRepository(requireContext()))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWordDetailBinding.bind(view)

        val wordId = arguments?.getLong(WordListFragment.ARG_WORD_ID, -1L) ?: -1L
        viewModel.loadWord(wordId)

        setupActions()
        setupEmptyState()
        observeState()
        observeEvents()
    }

    private fun setupActions() {
        binding.detailEditButton.setOnClickListener {
            val word = viewModel.uiState.value.word ?: return@setOnClickListener
            WordEditorDialog.show(requireContext(), layoutInflater, word) { newWord, newMeaning ->
                viewModel.updateWord(word, newWord, newMeaning)
            }
        }

        binding.detailDeleteButton.setOnClickListener {
            val word = viewModel.uiState.value.word ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_word)
                .setMessage(getString(R.string.dialog_delete_message, word.word))
                .setPositiveButton(R.string.action_delete) { _, _ ->
                    viewModel.deleteWord(word)
                }
                .setNegativeButton(R.string.action_cancel, null)
                .show()
        }

        binding.detailMasterButton.setOnClickListener {
            val word = viewModel.uiState.value.word ?: return@setOnClickListener
            viewModel.setMastered(word, !word.mastered)
        }
    }

    private fun setupEmptyState() {
        val emptyState = binding.root.findViewById<View>(R.id.empty_state)
        emptyState.findViewById<TextView>(R.id.empty_title).setText(R.string.empty_detail_title)
        emptyState.findViewById<TextView>(R.id.empty_message).setText(R.string.empty_detail_message)

        emptyState.findViewById<MaterialButton>(R.id.empty_primary_button).apply {
            setText(R.string.action_back_to_list)
            setOnClickListener { findNavController().navigateUp() }
        }
        emptyState.findViewById<MaterialButton>(R.id.empty_secondary_button).isVisible = false
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
                        WordDetailEvent.Deleted -> findNavController().navigateUp()
                        is WordDetailEvent.Error -> Snackbar.make(
                            binding.root,
                            event.message,
                            Snackbar.LENGTH_LONG,
                        ).show()
                    }
                }
            }
        }
    }

    private fun render(state: WordDetailUiState) {
        binding.root.findViewById<View>(R.id.loading_state).isVisible = state.isLoading
        binding.root.findViewById<View>(R.id.empty_state).isVisible =
            !state.isLoading && state.word == null
        binding.detailContent.isVisible = !state.isLoading && state.word != null

        state.word?.let(::bindWord)

        state.errorMessage?.let { message ->
            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
            viewModel.consumeError()
        }
    }

    private fun bindWord(word: Word) {
        with(binding) {
            detailWord.text = word.word
            detailMeaning.text = word.meaning
            detailStage.text = stageLabel(requireContext(), word.memoryStage)
            detailAddedValue.text = formatTimestamp(word.addedAt)
            detailReviewValue.text = formatTimestamp(word.reviewAt)
            detailStageValue.text = stageLabel(requireContext(), word.memoryStage)
            detailMasteredValue.setText(
                if (word.mastered) {
                    R.string.detail_mastered_yes
                } else {
                    R.string.detail_mastered_no
                },
            )
            detailMasterButton.setText(
                if (word.mastered) {
                    R.string.action_unmark_mastered
                } else {
                    R.string.action_mark_mastered
                },
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
