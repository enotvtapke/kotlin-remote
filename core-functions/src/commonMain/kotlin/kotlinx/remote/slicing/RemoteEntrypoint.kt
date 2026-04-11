package kotlinx.remote.slicing

import kotlinx.remote.RemoteConfig
import kotlin.reflect.KClass

/**
 * DO NOT USE IT!
 * Marks a function as a remote entrypoint. Accepts a list of remote configs. Functions marked with these remote configs will be preserved in compiled code for the marked entrypoint.
 * This annotation is for future development only. To support code slicing, compiler integration is needed, which is not implemented yet.
 */
@Target(AnnotationTarget.FUNCTION)
annotation class RemoteEntrypoint(val remoteConfigs: Array<KClass<RemoteConfig>>)
