package edu.unikom.herbamedjabar.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.auth.FirebaseAuth
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.data.Post
import edu.unikom.herbamedjabar.databinding.ItemPostBinding
import edu.unikom.herbamedjabar.util.MarkdownUtils

class PostAdapter(
    private val onLikeClicked: (String) -> Unit,
    private val onDeleteClicked: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = ItemPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }

    inner class PostViewHolder(private val binding: ItemPostBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(post: Post) {
            val currentUser = FirebaseAuth.getInstance().currentUser
            binding.tvUsername.text = post.username
            binding.ivUserProfile.load(post.userProfilePictureUrl) {
                placeholder(R.drawable.ic_user_image)
                error(R.drawable.ic_user_image)
            }
            binding.ivPostImage.load(post.imageUrl) {
                placeholder(R.drawable.bg_place_holder)
                error(R.drawable.bg_place_holder)
            }
            binding.tvPlantName.text = post.plantName
            binding.tvContent.text = HtmlCompat.fromHtml(
                MarkdownUtils.parseMarkdownToHtml(post.content), HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            binding.tvManfaat.text = HtmlCompat.fromHtml(
                MarkdownUtils.parseMarkdownToHtml(post.benefit, true), HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            binding.tvEfek.text = HtmlCompat.fromHtml(
                MarkdownUtils.parseMarkdownToHtml(post.warning, true), HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            binding.tvLikeCount.text = "${post.likes.size}"
            binding.ivLike.setImageResource(
                if (post.likes.contains(currentUser?.uid)) R.drawable.ic_heart_filled else R.drawable.ic_hearth_outline
            )
            binding.ivLike.setOnClickListener { onLikeClicked(post.id) }
            binding.ivMenuOptions.visibility =
                if (post.userId == currentUser?.uid) View.VISIBLE else View.GONE
            binding.ivMenuOptions.setOnClickListener { onDeleteClicked(post) }
            binding.tvPostTimestamp.text = android.text.format.DateUtils.formatDateTime(
                binding.root.context,
                post.timestamp,
                android.text.format.DateUtils.FORMAT_SHOW_DATE or
                    android.text.format.DateUtils.FORMAT_SHOW_YEAR or
                    android.text.format.DateUtils.FORMAT_SHOW_TIME
            )
            binding.ivUserProfile.contentDescription =
                binding.root.context.getString(R.string.cd_user_profile_of, post.username)
            binding.ivPostImage.contentDescription = post.plantName
            binding.ivLike.contentDescription = if (post.likes.contains(currentUser?.uid)) {
                binding.root.context.getString(R.string.cd_liked)
            } else {
                binding.root.context.getString(R.string.cd_like)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
