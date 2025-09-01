package edu.unikom.herbamedjabar.adapter

import android.text.Spanned
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.collection.LruCache
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.load
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.data.ScanHistory
import edu.unikom.herbamedjabar.databinding.ItemHistoryBinding
import edu.unikom.herbamedjabar.util.MarkdownUtils
import java.io.File

class HistoryAdapter(
    private val onClick: (ScanHistory) -> Unit
) :
    ListAdapter<ScanHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

    companion object {
        private const val HTML_CACHE_SIZE = 64_000
        private val htmlCache = object : LruCache<String, Spanned>(HTML_CACHE_SIZE) {
            override fun sizeOf(key: String, value: Spanned): Int = value.length
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding =
            ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
                val key = "${history.id}:${history.content.hashCode()}"
                val cached = htmlCache[key]
                val spanned = cached ?: run {
                    val html = MarkdownUtils.parseMarkdownToHtml(history.content)
                    HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)
                        .also { htmlCache.put(key, it) }
                }
                descriptionTextView.text = spanned

                time.text = android.text.format.DateUtils.formatDateTime(
                    binding.root.context,
                    history.timestamp,
                    android.text.format.DateUtils.FORMAT_SHOW_DATE or
                        android.text.format.DateUtils.FORMAT_SHOW_YEAR or
                        android.text.format.DateUtils.FORMAT_SHOW_TIME
                )

                val imageFile = File(history.imagePath)
                val data = if (imageFile.exists()) imageFile else R.drawable.bg_place_holder
                historyImageView.load(data, binding.root.context.imageLoader) {
                    crossfade(true)
                    placeholder(R.drawable.bg_place_holder)
                    error(R.drawable.bg_place_holder)
                    fallback(R.drawable.bg_place_holder)
                }
                historyImageView.contentDescription =
                    binding.root.context.getString(R.string.cd_plant_image_of, history.plantName)

                itemView.setOnClickListener {
                    onClick(history)
                }
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
