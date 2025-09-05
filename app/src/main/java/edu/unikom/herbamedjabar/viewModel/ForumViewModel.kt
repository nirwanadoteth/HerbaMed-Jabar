package edu.unikom.herbamedjabar.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.unikom.herbamedjabar.data.Post
import edu.unikom.herbamedjabar.repository.PostRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

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

    private val inFlightLikes = ConcurrentHashMap.newKeySet<String>()

    init {
        fetchPosts()
    }

    private fun fetchPosts() {
        viewModelScope.launch {
            postRepository
                .getPosts()
                .onStart { _isLoading.value = true }
                .catch { e -> _error.value = e.message }
                .onCompletion { _isLoading.value = false }
                .collect { postList ->
                    _posts.value = postList
                    _error.value = null
                }
        }
    }

    fun toggleLikeOnPost(postId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            if (!inFlightLikes.add(postId)) return@launch
            runCatching {
                withContext(Dispatchers.IO) {
                    postRepository.toggleLike(postId, userId)
                }
            }.onFailure {
                // TODO: report to UI/logger and/or emit a UI event
            }.onSuccess {
                inFlightLikes.remove(postId)
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
