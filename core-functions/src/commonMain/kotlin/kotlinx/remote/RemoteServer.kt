package kotlinx.remote

suspend fun invokeCallable(callable: RemoteCallable, remoteCall: RemoteCall): RemoteResponse<*> {
    return try {
        context(LocalContext) {
            RemoteResponse.Success(callable.invokator.callWithUniqueName(remoteCall.arguments))
        }
    } catch (e: Exception) {
        RemoteResponse.Failure(e)
    }
}
