package social.server

import social.api.PostService
import social.api.SearchService
import social.api.UserService
import social.model.Post
import social.model.User

class SearchServiceImpl(
    private val postService: PostService,
    private val userService: UserService,
) : SearchService {
    override suspend fun searchPosts(query: String): List<Post> =
        postService.allPosts().filter { it.text.contains(query, ignoreCase = true) }

    override suspend fun searchUsers(query: String): List<User> = userService.searchByName(query)

    override suspend fun searchByHashtag(tag: String): List<Post> {
        val needle = "#$tag"
        return postService.allPosts().filter { it.text.contains(needle, ignoreCase = true) }
    }
}
