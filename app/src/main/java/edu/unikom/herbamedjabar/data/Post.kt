package edu.unikom.herbamedjabar.data

data class Post(
    val id: String = "",
    val userId: String = "",
    val username: String = "",
    val userProfilePictureUrl: String? = null,
    val imageUrl: String = "",
    val plantName: String = "",
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: List<String> = emptyList(),

    val content: String? = null,
    val benefit: String? = null,
    val warning: String? = null

)
