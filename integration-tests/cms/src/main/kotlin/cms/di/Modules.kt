package cms.di

import cms.repository.ArticleRepository
import cms.repository.CommentRepository
import cms.repository.UserStore
import org.koin.core.context.GlobalContext
import org.koin.dsl.module

val cmsModule = module {
    single { UserStore() }
    single { ArticleRepository() }
    single { CommentRepository() }
}

inline fun <reified T : Any> dep(): T = GlobalContext.get().get()
