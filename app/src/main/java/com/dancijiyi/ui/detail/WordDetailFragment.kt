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
import com.dancijiyi.databinding.FragmentWordDetailBinding
import com.dancijiyi.ui.common.ScreenState
import com.dancijiyi.ui.wordlist.WordListFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class WordDetailFragment : Fragment(R.layout.fragment_word_detail) {

    private var _binding: FragmentWordDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWordDetailBinding.bind(view)

        val wordId = arguments?.getLong(WordListFragment.ARG_WORD_ID, -1L) ?: -1L
        viewModel.loadWord(wordId)

        setupActions()
        setupEmptyState()
        observeUiState()
    }

    private fun setupActions() {
        binding.detailReviewButton.setOnClickListener {
            findNavController().navigate(R.id.action_wordDetail_to_review)
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
        binding.detailContent.isVisible = state == ScreenState.CONTENT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
