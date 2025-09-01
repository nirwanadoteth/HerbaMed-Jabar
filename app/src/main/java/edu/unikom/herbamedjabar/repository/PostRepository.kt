package edu.unikom.herbamedjabar.repository

import android.net.Uri
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import edu.unikom.herbamedjabar.data.Post
import edu.unikom.herbamedjabar.util.PlantData
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun getPosts(): Flow<List<Post>> = callbackFlow {
        val collection = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val posts = snapshot.toObjects(Post::class.java)
                trySend(posts).isSuccess
            }
        }
        awaitClose { listener.remove() }
    }

    fun getPostsByUserId(userId: String): Flow<List<Post>> = callbackFlow {
        val collection = firestore.collection("posts")
            .whereEqualTo("userId", userId)

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val posts = snapshot.toObjects(Post::class.java)
                val sortedPosts = posts.sortedByDescending { it.timestamp }
                trySend(sortedPosts).isSuccess
            }
        }
        awaitClose { listener.remove() }
    }


    suspend fun createPost(
        userId: String,
        username: String,
        userProfilePictureUrl: String?,
        imageUri: Uri,
        plantName: String,
        description: String,
        parsedData: PlantData,
    ) {
        val imageUrl = uploadImageToCloudinary(imageUri)
        val postId = firestore.collection("posts").document().id
        val newPost = Post(
            id = postId,
            userId = userId,
            username = username,
            userProfilePictureUrl = userProfilePictureUrl,
            imageUrl = imageUrl,
            plantName = plantName,
            description = description,
            timestamp = System.currentTimeMillis(),
            benefit = parsedData.benefit,
            warning = parsedData.warning,
            content = parsedData.description
        )
        firestore.collection("posts").document(postId).set(newPost).await()
    }

    suspend fun toggleLike(postId: String, userId: String) {
        val postRef = firestore.collection("posts").document(postId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likes = snapshot.get("likes") as? List<String> ?: emptyList()
            if (likes.contains(userId)) {
                transaction.update(postRef, "likes", FieldValue.arrayRemove(userId))
            } else {
                transaction.update(postRef, "likes", FieldValue.arrayUnion(userId))
            }
        }.await()
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri): String =
        suspendCancellableCoroutine { continuation ->
            val requestId = MediaManager.get().upload(imageUri)
                .callback(object : UploadCallback {
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as? String
                        if (url != null && continuation.isActive) {
                            continuation.resume(url)
                        } else if (continuation.isActive) {
                            continuation.resumeWithException(Exception("Cloudinary upload failed: URL is null"))
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(Exception("Cloudinary Error: ${error.description}"))
                        }
                    }

                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()

            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }

    suspend fun deletePost(post: Post) {
        // Hanya hapus dokumen dari Firestore
        firestore.collection("posts").document(post.id).delete().await()
    }

}
