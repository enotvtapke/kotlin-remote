package social.repository

import java.util.concurrent.atomic.AtomicLong
import social.model.CreatePostDto
import social.model.EditPostDto
import social.model.Post

class PostRepository {
    private val nextId = AtomicLong(1)
    private val posts = mutableMapOf<Long, Post>()

    fun createPost(dto: CreatePostDto): Post {
        val post = Post(nextId.getAndIncrement(), dto.authorId, dto.text)
        posts[post.id] = post
        return post
    }

    fun getPost(id: Long): Post = posts[id] ?: error("Post $id not found")

    fun listUserPosts(authorId: Long): List<Post> =
        posts.values.filter { it.authorId == authorId }.sortedByDescending { it.createdAt }

    fun deletePost(id: Long) {
        posts.remove(id) ?: error("Post $id not found")
    }

    fun editPost(dto: EditPostDto): Post {
        val p = getPost(dto.id)
        val updated = p.copy(text = dto.text)
        posts[dto.id] = updated
        return updated
    }

    fun allPosts(): List<Post> = posts.values.toList()
}
