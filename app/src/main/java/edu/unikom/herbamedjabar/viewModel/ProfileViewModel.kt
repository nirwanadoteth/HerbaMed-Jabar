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
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ProfileViewModel
@Inject
constructor(
    private val auth: FirebaseAuth,
    private val postRepository: PostRepository, // Inject PostRepository
) : ViewModel() {

    private var userPostsJob: Job? = null

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _userPosts = MutableLiveData<List<Post>>(emptyList())
    val userPosts: LiveData<List<Post>> = _userPosts

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        _user.value = getCurrentUser()
        fetchUserPosts()
    }

    private val inFlightLikes = ConcurrentHashMap.newKeySet<String>()

    private fun fetchUserPosts() {
        val userId = auth.currentUser?.uid ?: return
        userPostsJob?.cancel()
        userPostsJob =
            viewModelScope.launch {
                postRepository
                    .getPostsByUserId(userId)
                    .onStart { _isLoading.value = true }
                    .catch { e ->
                        _error.value = e.message ?: "Unexpected error"
                        _isLoading.value = false
                    }
                    .collectLatest { postList ->
                        _error.value = null
                        _userPosts.value = postList
                        _isLoading.value = false
                    }
            }
    }

    fun toggleLikeOnPost(postId: String) {
        viewModelScope.launch {
            val userId =
                getCurrentUserId()
                    ?: run {
                        _error.value = AUTH_REQUIRED_ERROR
                        return@launch
                    }
            if (!inFlightLikes.add(postId)) return@launch
            try {
                withContext(Dispatchers.IO) { postRepository.toggleLike(postId, userId) }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _error.value = e.message ?: "Unexpected error"
            } finally {
                inFlightLikes.remove(postId)
            }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { postRepository.deletePost(post) }
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Exception) {
                _error.value = e.message ?: "Unexpected error"
            }
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun isUserAuthenticated(): Boolean = auth.currentUser != null

    fun signOut() {
        auth.signOut()
        userPostsJob?.cancel()
        _user.value = null
        _userPosts.value = emptyList()
    }

    fun logout() {
        signOut()
    }

    override fun onCleared() {
        userPostsJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val AUTH_REQUIRED_ERROR = "auth_required"
    }
}
