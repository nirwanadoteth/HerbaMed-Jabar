package edu.unikom.herbamedjabar.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.google.firebase.auth.FirebaseUser
import edu.unikom.herbamedjabar.R
import edu.unikom.herbamedjabar.data.Post
import edu.unikom.herbamedjabar.databinding.ItemPostBinding
import edu.unikom.herbamedjabar.util.MarkdownUtils

class PostAdapter(
    private val onLikeClicked: (String) -> Unit,
    private val onDeleteClicked: (Post) -> Unit,
    private val currentUser: FirebaseUser?
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
            val user = this@PostAdapter.currentUser
            binding.tvUsername.text = post.username
            binding.ivUserProfile.load(post.userProfilePictureUrl) {
                crossfade(true)
                placeholder(R.drawable.ic_user_image)
                error(R.drawable.ic_user_image)
                fallback(R.drawable.ic_user_image)
            }
            binding.ivPostImage.load(post.imageUrl) {
                crossfade(true)
                placeholder(R.drawable.bg_place_holder)
                error(R.drawable.bg_place_holder)
                fallback(R.drawable.bg_place_holder)
            }
            binding.plantNameTextView.text = post.plantName

            // Use content-aware cache keys to avoid stale parses across updates.
            val contentText = post.content
            val benefitText = post.benefit.orEmpty()
            val warningText = post.warning.orEmpty()

            val contentKey = "post:${post.id}:content:${contentText.hashCode()}"
            val benefitKey = "post:${post.id}:benefit:${benefitText.hashCode()}"
            val warningKey = "post:${post.id}:warning:${warningText.hashCode()}"

            val contentSpanned = MarkdownUtils.parseMarkdownToSpanned(contentText, contentKey)
            val benefitSpanned =
                MarkdownUtils.parseMarkdownToSpanned(benefitText, benefitKey, true)
            val warningSpanned =
                MarkdownUtils.parseMarkdownToSpanned(warningText, warningKey, true)

            binding.contentTextView.text = contentSpanned
            binding.benefitTextView.text = benefitSpanned
            binding.warningTextView.text = warningSpanned
            binding.benefitCard.visibility =
                if (post.benefit.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.warningCard.visibility =
                if (post.warning.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.tvLikeCount.text = "${post.likes.size}"
            val likedByMe = user?.uid?.let(post.likes::contains) == true
            binding.btnLike.isChecked = likedByMe
            binding.btnLike.setOnClickListener {
                onLikeClicked(post.id)
            }
            binding.btnMenuOptions.visibility =
                if (post.userId == user?.uid) View.VISIBLE else View.GONE
            binding.btnMenuOptions.setOnClickListener { onDeleteClicked(post) }
            val tsMillis =
                if (post.timestamp in 1 until 1_000_000_000_000L) post.timestamp * 1000
                else post.timestamp
            binding.tvPostTimestamp.text =
                if (tsMillis > 0)
                    DateUtils.getRelativeTimeSpanString(
                        tsMillis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
                    ).toString()
                else ""
            binding.ivUserProfile.contentDescription =
                binding.root.context.getString(R.string.cd_user_profile_of, post.username)
            binding.ivPostImage.contentDescription =
                binding.root.context.getString(R.string.cd_plant_image_of, post.plantName)
            binding.btnLike.contentDescription =
                if (likedByMe) {
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
