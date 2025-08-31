package edu.unikom.herbamedjabar.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.unikom.herbamedjabar.data.Post
import edu.unikom.herbamedjabar.repository.PostRepository
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val postRepository: PostRepository // Inject PostRepository
) : ViewModel() {

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    init {
        _user.value = auth.currentUser
        fetchUserPosts()
    }

    private fun fetchUserPosts() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            postRepository.getPostsByUserId(userId).collect { posts ->
                _userPosts.value = posts
            }
        }
    }

    fun toggleLikeOnPost(postId: String) {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch
                postRepository.toggleLike(postId, userId)
            } catch (e: Exception) {
                android.util.Log.w("ProfileViewModel", "toggleLikeOnPost failed", e)
            }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(post)
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "deletePost failed", e)
            }
        }
    }

    fun logout() {
        auth.signOut()
    }
}
