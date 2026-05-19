package pubsub.di

import org.koin.core.context.GlobalContext
import org.koin.dsl.module
import pubsub.repository.BusState

val busModule = module { single { BusState() } }
val subscriberModule = module { }

inline fun <reified T : Any> dep(): T = GlobalContext.get().get()
