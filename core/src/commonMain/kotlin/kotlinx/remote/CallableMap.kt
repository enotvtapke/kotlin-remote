package kotlinx.remote


object CallableMap {
    // This map is generated during compilation
    var callableMap: MutableMap<String, RemoteCallable> = mutableMapOf()

    fun init(newMap: MutableMap<String, RemoteCallable> = mutableMapOf()) {
        callableMap = newMap
    }

    operator fun get(name: String): RemoteCallable = callableMap[name]
        ?: throw IllegalStateException(
            "Function $name is not registered in CallableMap. Registered functions: ${callableMap.keys.joinToString()}."
        )
    operator fun set(name: String, callable: RemoteCallable) { callableMap[name] = callable }
}