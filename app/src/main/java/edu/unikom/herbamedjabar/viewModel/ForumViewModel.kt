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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
                .catch { e ->
                    _error.value = e.message
                    _isLoading.value = false
                }
                .collectLatest { postList ->
                    _posts.value = postList
                    _error.value = null
                    _isLoading.value = false
                }
        }
    }

    fun toggleLikeOnPost(postId: String) {
        viewModelScope.launch {
            val userId =
                auth.currentUser?.uid
                    ?: run {
                        _error.value = AUTH_REQUIRED_ERROR /* expose a constant or sealed type */
                        return@launch
                    }
            if (!inFlightLikes.add(postId)) return@launch
            try {
                withContext(Dispatchers.IO) { postRepository.toggleLike(postId, userId) }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                _error.value = t.message
            } finally {
                inFlightLikes.remove(postId)
            }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { postRepository.deletePost(post) } }
                .onFailure {
                    when (it) {
                        is CancellationException -> throw it
                        else -> _error.value = it.message
                    }
                }
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    companion object { const val AUTH_REQUIRED_ERROR = "auth_required" }
}
