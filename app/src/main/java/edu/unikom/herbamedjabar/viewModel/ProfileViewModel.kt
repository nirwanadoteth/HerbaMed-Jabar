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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

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

    private val _userPosts = MutableLiveData<List<Post>>()
    val userPosts: LiveData<List<Post>> = _userPosts

    init {
        _user.value = auth.currentUser
        fetchUserPosts()
    }

    private val inFlightLikes = ConcurrentHashMap.newKeySet<String>()

    private fun fetchUserPosts() {
        val userId = auth.currentUser?.uid ?: return
        userPostsJob?.cancel()
        userPostsJob = viewModelScope.launch {
            postRepository.getPostsByUserId(userId)
                .catch { /* TODO: report to UI/logger */ }
                .collect { posts -> _userPosts.value = posts }
        }
    }

    fun toggleLikeOnPost(postId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            if (!inFlightLikes.add(postId)) return@launch
            runCatching {
                    withContext(Dispatchers.IO) { postRepository.toggleLike(postId, userId) }
                }
                .onFailure {
                    // TODO: report to UI/logger and/or emit a UI event
                }
                .onSuccess { inFlightLikes.remove(postId) }
        }
    }

    fun deletePost(post: Post) {
        viewModelScope.launch {
            runCatching { withContext(Dispatchers.IO) { postRepository.deletePost(post) } }
                .onFailure {
                    // TODO: report to UI/logger and/or emit a UI event
                }
        }
    }

    fun logout() {
        auth.signOut()
        userPostsJob?.cancel()
        _user.value = null
        _userPosts.value = emptyList()
    }
}
