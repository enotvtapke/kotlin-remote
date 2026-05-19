package social.server

import social.api.PostService
import social.model.CreatePostDto
import social.model.EditPostDto
import social.model.Post
import social.repository.PostRepository

class PostServiceImpl(
    private val repo: PostRepository,
) : PostService {
    override suspend fun createPost(dto: CreatePostDto): Post = repo.createPost(dto)
    override suspend fun getPost(id: Long): Post = repo.getPost(id)
    override suspend fun listUserPosts(authorId: Long): List<Post> = repo.listUserPosts(authorId)
    override suspend fun deletePost(id: Long) = repo.deletePost(id)
    override suspend fun editPost(dto: EditPostDto): Post = repo.editPost(dto)
    override suspend fun allPosts(): List<Post> = repo.allPosts()
}
