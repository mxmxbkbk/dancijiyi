package com.dancijiyi.ui.common

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.dancijiyi.R
import com.dancijiyi.data.Word
import com.dancijiyi.databinding.DialogWordEditorBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * 新增/编辑单词共用的轻量对话框。只做输入校验与回调，
 * 数据库写入由页面 ViewModel 完成。
 */
object WordEditorDialog {

    fun show(
        context: Context,
        inflater: LayoutInflater,
        existing: Word? = null,
        onSave: (word: String, meaning: String) -> Unit,
    ) {
        val binding = DialogWordEditorBinding.inflate(inflater)
        existing?.let {
            binding.wordInput.setText(it.word)
            binding.meaningInput.setText(it.meaning)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(
                if (existing == null) R.string.dialog_add_word else R.string.dialog_edit_word,
            )
            .setView(binding.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val word = binding.wordInput.text?.toString()?.trim().orEmpty()
                val meaning = binding.meaningInput.text?.toString()?.trim().orEmpty()

                binding.wordLayout.error = null
                binding.meaningLayout.error = null

                var valid = true
                if (word.isEmpty()) {
                    binding.wordLayout.error = context.getString(R.string.error_word_required)
                    valid = false
                }
                if (meaning.isEmpty()) {
                    binding.meaningLayout.error = context.getString(R.string.error_meaning_required)
                    valid = false
                }

                if (valid) {
                    onSave(word, meaning)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }
}
