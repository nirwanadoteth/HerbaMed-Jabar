package edu.unikom.herbamedjabar.viewModel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import edu.unikom.herbamedjabar.repository.PostRepository
import edu.unikom.herbamedjabar.util.PlantDataParser
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultViewModel
@Inject
constructor(private val postRepository: PostRepository, private val auth: FirebaseAuth) :
    ViewModel() {

    private val _postResult = MutableLiveData<Result<Unit>>()
    val postResult: LiveData<Result<Unit>> = _postResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun createPostFromScan(imageUri: Uri, plantName: String, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val result = runCatching {
                val user = checkNotNull(auth.currentUser) { "User not logged in" }
                val parsedData = PlantDataParser.parsePlantData(description)
                postRepository.createPost(
                    userId = user.uid,
                    username = user.displayName ?: "Anonymous",
                    userProfilePictureUrl = user.photoUrl?.toString(),
                    imageUri = imageUri,
                    plantName = plantName,
                    description = description,
                    parsedData = parsedData,
                )
            }
            _postResult.value = result.map {}
            _isLoading.value = false
        }
    }
}
