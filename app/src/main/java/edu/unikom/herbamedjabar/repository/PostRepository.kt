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
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    data class UploadResult(val url: String, val publicId: String?)
    class UploadException(val code: Int?, message: String) : Exception(message)

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
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(QUERY_LIMIT)

        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                trySend(snapshot.toObjects(Post::class.java)).isSuccess
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
            imageUrl = imageUrl.url,
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

    companion object {
        private const val CLOUDINARY_UPLOAD_TIMEOUT_MS = 60_000L
        private const val QUERY_LIMIT = 50L
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri): UploadResult =
        withTimeout(CLOUDINARY_UPLOAD_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                var requestId: String? = null
                val uploader = MediaManager.get().upload(imageUri)
                    .callback(object : UploadCallback {
                        override fun onStart(rId: String) {
                            requestId = rId
                        }

                        override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                            val url = (resultData["secure_url"] ?: resultData["url"]) as? String
                            val publicId = resultData["public_id"] as? String
                            if (url != null && continuation.isActive) {
                                continuation.resume(UploadResult(url, publicId))
                            } else if (continuation.isActive) {
                                continuation.resumeWithException(
                                    UploadException(
                                        null,
                                        "Cloudinary upload failed: URL is null (requestId=$requestId)"
                                    )
                                )
                            }
                        }

                        override fun onError(requestId: String, error: ErrorInfo) {
                            if (continuation.isActive) {
                                continuation.resumeWithException(
                                    UploadException(
                                        error.code,
                                        "Cloudinary: ${error.description} (requestId=$requestId)"
                                    )
                                )
                            }
                        }

                        @Suppress("EmptyFunctionBlock")
                        override fun onProgress(
                            requestId: String,
                            bytes: Long,
                            totalBytes: Long
                        ) {}

                        @Suppress("EmptyFunctionBlock")
                        override fun onReschedule(
                            requestId: String,
                            error: ErrorInfo
                        ) {}
                    })
                continuation.invokeOnCancellation {
                    requestId?.let { MediaManager.get().cancelRequest(it) }
                }
                uploader.dispatch()
            }
        }

    suspend fun deletePost(post: Post) {
        firestore.collection("posts").document(post.id).delete().await()
    }
}
