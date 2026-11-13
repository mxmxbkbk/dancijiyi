package com.dancijiyi.ui.wordlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dancijiyi.data.Word
import com.dancijiyi.databinding.ItemWordBinding
import com.dancijiyi.ui.common.dueLabel
import com.dancijiyi.ui.common.stageLabel

/**
 * 单词列表适配器：只负责展示与点击回调，不持有业务状态。
 */
class WordAdapter(
    private val onClick: (Word) -> Unit,
) : ListAdapter<Word, WordAdapter.WordViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WordViewHolder {
        val binding = ItemWordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return WordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WordViewHolder, position: Int) {
        val word = getItem(position)
        with(holder.binding) {
            itemWord.text = word.word
            itemMeaning.text = word.meaning
            itemStage.text = stageLabel(root.context, word.memoryStage)
            itemDue.text = dueLabel(root.context, word)
            root.setOnClickListener { onClick(word) }
        }
    }

    class WordViewHolder(
        val binding: ItemWordBinding,
    ) : RecyclerView.ViewHolder(binding.root)

    private object DiffCallback : DiffUtil.ItemCallback<Word>() {
        override fun areItemsTheSame(oldItem: Word, newItem: Word): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Word, newItem: Word): Boolean =
            oldItem == newItem
    }
}
