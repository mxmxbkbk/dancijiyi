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
import com.dancijiyi.databinding.FragmentWordListBinding
import com.dancijiyi.ui.common.ScreenState
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class WordListFragment : Fragment(R.layout.fragment_word_list) {

    private var _binding: FragmentWordListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: WordListViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentWordListBinding.bind(view)
        setupEmptyState()
        observeUiState()
    }

    private fun setupEmptyState() {
        val emptyState = binding.root.findViewById<View>(R.id.empty_state)
        emptyState.findViewById<TextView>(R.id.empty_title).setText(R.string.empty_list_title)
        emptyState.findViewById<TextView>(R.id.empty_message).setText(R.string.empty_list_message)

        emptyState.findViewById<MaterialButton>(R.id.empty_primary_button).apply {
            setText(R.string.action_import)
            setOnClickListener {
                findNavController().navigate(R.id.action_wordList_to_import)
            }
        }
        emptyState.findViewById<MaterialButton>(R.id.empty_secondary_button).apply {
            setText(R.string.action_preview_detail)
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_wordList_to_wordDetail,
                    Bundle().apply { putLong(ARG_WORD_ID, -1L) },
                )
            }
        }
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
        binding.listContent.isVisible = state == ScreenState.CONTENT
        binding.addWordFab.isVisible = state == ScreenState.CONTENT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_WORD_ID = "wordId"
    }
}
