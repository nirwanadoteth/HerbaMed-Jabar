package edu.unikom.herbamedjabar.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.unikom.herbamedjabar.data.Post
import edu.unikom.herbamedjabar.repository.PostRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class ForumViewModel
@Inject
constructor(private val postRepository: PostRepository, private val auth: FirebaseAuth) :
    ViewModel() {

    private val _posts = MutableLiveData<List<Post>>()
    val posts: LiveData<List<Post>> = _posts

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        fetchPosts()
    }

    private fun fetchPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            postRepository
                .getPosts()
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collect { postList ->
                    _posts.value = postList
                    _isLoading.value = false
                    _error.value = null
                }
        }
    }

    fun toggleLikeOnPost(postId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                postRepository.toggleLike(postId, userId)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            try {
                postRepository.deletePost(post)
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
