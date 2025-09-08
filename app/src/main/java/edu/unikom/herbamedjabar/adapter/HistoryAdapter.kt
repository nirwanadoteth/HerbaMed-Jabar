package edu.unikom.herbamedjabar.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.data.ScanHistory
import edu.unikom.herbamedjabar.databinding.ItemHistoryBinding
import edu.unikom.herbamedjabar.util.MarkdownUtils
import java.io.File

class HistoryAdapter(private val onClick: (ScanHistory, android.widget.ImageView) -> Unit) :
    ListAdapter<ScanHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id.toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = getItem(position)
        holder.bind(historyItem)
    }

    inner class HistoryViewHolder(private val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(history: ScanHistory) {
            binding.apply {
                plantNameTextView.text = history.plantName

                val key = "history:${history.id}:content:${history.content.hashCode()}"
                val spanned = MarkdownUtils.parseMarkdownToSpanned(history.content, key)
                descriptionTextView.text = spanned

                timeTextView.text =
                    android.text.format.DateUtils.formatDateTime(
                        binding.root.context,
                        history.timestamp,
                        android.text.format.DateUtils.FORMAT_SHOW_DATE or
                            android.text.format.DateUtils.FORMAT_SHOW_YEAR or
                            android.text.format.DateUtils.FORMAT_SHOW_TIME,
                    )

                val data =
                    history.imagePath
                        .takeIf { it.isNotBlank() }
                        ?.let { path -> File(path).takeIf { it.exists() } }
                        ?: R.drawable.bg_place_holder
                historyImageView.load(data) {
                    crossfade(true)
                    placeholder(R.drawable.bg_place_holder)
                    error(R.drawable.bg_place_holder)
                    fallback(R.drawable.bg_place_holder)
                }
                val ctx = binding.root.context
                val cdText = history.plantName.ifBlank { ctx.getString(R.string.cd_plant_image) }
                historyImageView.contentDescription =
                    if (history.plantName.isBlank()) cdText
                    else ctx.getString(R.string.cd_plant_image_of, history.plantName)

                // Set transition name for shared element
                historyImageView.transitionName = "historyImage_${history.id}"

                // Set click listener for shared element transition
                itemView.setOnClickListener { onClick(history, historyImageView) }
            }
        }
    }
}

class HistoryDiffCallback : DiffUtil.ItemCallback<ScanHistory>() {
    override fun areItemsTheSame(oldItem: ScanHistory, newItem: ScanHistory): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ScanHistory, newItem: ScanHistory): Boolean {
        return oldItem == newItem
    }
}
