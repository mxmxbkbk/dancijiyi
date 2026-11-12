package com.dancijiyi.ui.review

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
import com.dancijiyi.databinding.FragmentReviewBinding
import com.dancijiyi.ui.common.ScreenState
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ReviewFragment : Fragment(R.layout.fragment_review) {

    private var _binding: FragmentReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReviewViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentReviewBinding.bind(view)
        setupEmptyState()
        observeUiState()
    }

    private fun setupEmptyState() {
        val emptyState = binding.root.findViewById<View>(R.id.empty_state)
        emptyState.findViewById<TextView>(R.id.empty_title).setText(R.string.empty_review_title)
        emptyState.findViewById<TextView>(R.id.empty_message).setText(R.string.empty_review_message)

        emptyState.findViewById<MaterialButton>(R.id.empty_primary_button).apply {
            setText(R.string.action_back_to_home)
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
        binding.reviewContent.isVisible = state == ScreenState.CONTENT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
