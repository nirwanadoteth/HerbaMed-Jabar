package edu.unikom.herbamedjabar.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.data.ScanHistory
import edu.unikom.herbamedjabar.databinding.ItemHistoryBinding
import edu.unikom.herbamedjabar.util.MarkdownUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onClick: (ScanHistory) -> Unit
) :
    ListAdapter<ScanHistory, HistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {

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
        fun bind(history: ScanHistory) { // Terima posisi di sini
            binding.apply {
                plantNameTextView.text = history.plantName
                val html = MarkdownUtils.parseMarkdownToHtml(history.content)
                descriptionTextView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY)

                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                val date = Date(history.timestamp)
                time.text = sdf.format(date)

                val imageFile = File(history.imagePath)
                if (imageFile.exists()) {
                    historyImageView.load(Uri.fromFile(imageFile)) {
                        crossfade(true)
                        placeholder(R.drawable.bg_place_holder)
                    }
                }

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
