## Kotlin Remote

### Goals

* Develop a technology for network interactions that does not require to write boilerplate code

### Objectives

* Make possible to call Kotlin functions from application on different machine
* Call Kotlin class constructors and methods from application on different machine

### Idea and intuition behind

Idea is to allow calling usual kotlin functions from another machine. To make it possible, both machines have to use the
same codebase. Network interactions itself are of no interest in this thesis and implemented using a Ktor framework and
HTTP protocol.

Now let us take a look at the example. Imagine we have a simple function `multiply`, that we want to call not in the
running program but rather on another machine. We will be calling such functions _remote_ functions.

```kotlin
fun multiply(lhs: Long, rhs: Long) = lhs * rhs
```

To make it possible, three things are required:

* Function should be able to make network calls
* It should be known what machine such a call will target
* It should be possible to determine whether the function will be called on a remote machine or not

To satisfy these requirements function `multiply` can be rewritten in the following way:

```kotlin
context(ctx: RemoteContext)
@Remote(ServerConfig::class)
suspend fun multiply(lhs: Long, rhs: Long) =
    if (ctx == ServerConfig.context) {
        lhs * rhs
    } else {
        ServerConfig.client.call<Long>(
            RemoteCall("multiply", arrayOf(lhs, rhs))
        )
    }
```

Here the context parameter `ctx` is used to determine whether the function should be called on another machine or not.
When a function is called on another machine, we will be saying that this function is called _remotely_ or that a
function call results in a _remote call_. We will be calling the target machine of such a call _remote machine_. So
when function `multiple` is called in a `ServerConfig.context` it will be called locally, and when in another context it
will be called remotely.

For example, here function `multiply` is called locally:

```kotlin
context(ClientContext) {
    println(multiply(6, 5))
}
```

And here it is called remotely:

```kotlin
context(ServerConfig.context) {
    println(multiply(6, 5))
}
```

`ServerConfig` determines not only in which context remote function is called locally but also what machine should be
used as a target for remote call. `ServerConfig.client` is just an extended Ktor HTTP client. This client contains IP of
the remote machine and supports very fine adjustments, like adding HTTP headers for authentication.

And `suspend` modifier is used so that the function can make network calls. This is needed because all the network calls
in Ktor are asynchronous.

### Remote function metadata

But there is another problem. To make network calls possible, all the function argument types and return value
types should be serializable. For serialization kotlix.serialization library is used. In the case of `multiply` all the
arguments and return value are of type `Long`, which is serializable by default.

Unfortunately, when a function is called remotely, it is unclear what serializers should be used to deserialize
arguments and serialize return value. To solve this problem a special storage with remote function metadata is used.
This storage contains for every remote function information about this function signature and a function reference to
call the function on the remote machine.

For `multiply` function it looks like this:

```kotlin
CallableMap["multiply"] = RemoteCallable(
    name = "multiply",
    returnType = RemoteType(typeOf<RemoteResponse<Long>>()),
    invokator = RemoteInvokator { args ->
        return@RemoteInvokator with(ServerConfig.context) {
            multiply(args[0] as Long, args[1] as Long)
        }
    },
    parameters = arrayOf(
        RemoteParameter("lhs", RemoteType(typeOf<Long>())),
        RemoteParameter("rhs", RemoteType(typeOf<Long>()))
    ),
)
```

Here `CallableMap` is a storage, `invokator` is basically a reference to `multiply` function. Server uses this reference
to call the function. It is worth noting that remote function inside `invokator` is always called locally.

### Code generation

Now we know how to implement remote function, but writing all the code manually is not very convenient. To make writing
remote functions easier, a compiler plugin is used. It generates all the necessary additional code for remote functions.

With a compiler plugin writing and calling remote functions is as simple as writing and calling regular functions.

For example, here is how `multiply` remote function is called:

```kotlin
@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

fun main(): Unit = runBlocking {
    CallableMap.putAll(genCallableMap())
    with(ClientContext) {
        println(multiply(5, 6))
    }
}
```

The compiler plugin rewrites remote function bodies and substitutes `genCallableMap()` with callable map entries
generated for all the function marked as `@Remote(..)`. User still needs to provide a context to call remote functions,
because only the user knows should a remote function be called remotely or locally.

User should also set up `ServerConfig`. For example, in the following way:

```kotlin
data object ServerContext : RemoteContext
data object ClientContext : RemoteContext

data object ServerConfig : RemoteConfig {
    override val context = ServerContext
    override val client: RemoteClient = HttpClient {
        defaultRequest {
            url("http://localhost:8080")
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        install(ContentNegotiation) {
            json()
        }
    }.remoteClient("/call")
}
```

And write a code for the server:

```kotlin
fun main() {
    CallableMap.putAll(genCallableMap())
    embeddedServer(Netty, port = 8080) {
        install(CallLogging)
        install(ServerContentNegotiation) {
            json()
        }
        install(KRemote)
        routing {
            this.remote("/call")
        }
    }.start(wait = true)
}
```

Here user can setup Ktor server and client arbitrarily. That essentially means that everything that can be done with
Ktor and kotlinx.serialization can be done with remote functions.

### What functions can be remote?

Not just simple top level function can be marked as remote. Remote function can be nested inside other functions, they
can be class methods or extension functions, they can be generic, can throw exceptions that will be rethrown on the
client. Remote functions can also be recursive (direct or indirect), in this case recursive calls are made locally. All
the following functions are valid remote functions:

```kotlin
@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun divide(lhs: Long, rhs: Long): Long =
    if (rhs == 0) throw ArithmeticException("Division by zero") else lhs / rhs

@Remote(ServerConfig::class)
context(_: RemoteContext)
fun <T : Comparable<T>> Iterable<T>.maxOrNull(): T? { /* ... */
}

class Calculator(private var init: Int) {
    @Remote(ServerConfig::class)
    context(_: RemoteContext)
    suspend fun multiply(x: Int): Int {
        init *= x
        return init
    }
}

fun main(): Unit = runBlocking {
    CallableMap.putAll(genCallableMap())
    @Remote(ServerConfig::class)
    context(ctx: RemoteContext)
    suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs
    with(ClientContext) {
        println(multiply(5, 6))
    }
}

@Remote(ServerConfig::class)
context(_: RemoteContext, k: String)
suspend fun Long.times(rhs: Long) = this * rhs * k

@Remote(ServerConfig::class)
context(_: RemoteContext)
suspend fun fibonacciRecursive(n: Int): Long {
    if (n < 0) {
        throw IllegalArgumentException("n must be non-negative")
    }
    if (n <= 1) {
        return n.toLong()
    }
    return fibonacciRecursive(n - 1) + fibonacciRecursive(n - 2) // Called locally
}
```

### Remote classes

It was said that remote functions can be class methods. Static and non-static. This is implemented by treating
implicit `this` parameter of methods as a first argument of remote function in case of non-static methods.
When method is static, in other words its dispatch receiver is an object, than its `this` parameter will not be
transferred on the network. When method is not static than `this` parameter must be serializable. But writing your own 
serializer for the class is not always convenient. That is why special serializers for classes can be used, that 
serialize class instances as a single long value. This is how it works.

```kotlin
@Serializable(with = Calculator.CalculatorSerializer::class)
open class Calculator private constructor(private var init: Int) {
    @Remote(ServerConfig::class)
    context(_: RemoteContext)
    open suspend fun multiply(x: Int): Int {
        init *= x
        return init
    }

    class CalculatorStub(val id: Long) : Calculator(0)

    object CalculatorSerializer : KSerializer<Calculator> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("id", LONG)

        override fun serialize(encoder: Encoder, value: Calculator) {
            if (value is CalculatorStub) {
                encoder.encodeLong(value.id)
                return
            }
            val id = StubIdGenerator.nextId()
            instances[id] = value
            encoder.encodeLong(id)
        }

        override fun deserialize(decoder: Decoder): Calculator {
            val id = decoder.decodeLong()
            return instances[id]?.let { it as Calculator } ?: CalculatorStub(id)
        }
    }

    companion object {
        @Remote(ServerConfig::class)
        context(_: RemoteContext)
        suspend operator fun invoke(init: Int) = Calculator(init)
    }
}

fun main(): Unit = runBlocking {
    CallableMap.putAll(genCallableMap())
    context(ClientContext) {
        val x = Сalculator(5)
        println(x.multiply(6))
        println(x.multiply(7))
        println(x.result())
    }
}
```

Because constructors cannot have context parameters, remote classes should be instantiated with a factory function.
The client in the main function works with a stub of the class. `instances` map is a special storage where all the
remote class instances are stored. As for now there are no means by which this storage can be cleaned up.

Compiler plugin generates serializers and stubs automatically.

```kotlin
@RemoteSerializable
@Serializable(with = Calculator.RemoteClassSerializer::class)
class Calculator private constructor(private var init: Int) {
    @Remote(ServerConfig::class)
    context(_: RemoteContext)
    suspend fun multiply(x: Int): Int {
        init *= x
        return init
    }

    @Remote(ServerConfig::class)
    context(_: RemoteContext)
    suspend fun result(): Int {
        return init
    }

    companion object {
        @Remote(ServerConfig::class)
        context(_: RemoteContext)
        suspend operator fun invoke(init: Int) = Calculator(init)
    }
}
```

### Features

The project is completely kotlin multiplatform and supports all the KMP compilation targets.

### Current state

All the features described above are implemented and to some extent tested.

* The first thesis objective is reached.
* The second objective is almost reached. Manual or automatic memory management of remote class instances is still
  under development.

### Problems and open questions

* At the moment there is no way to remove instances from `instances` storage. This should be fixed by introducing some
  kind of garbage collector or at least manual removal.
* The support for generic functions could be improved. Now the upper bounds of all the generic function type parameters
  should be serializable, and it is the responsibility of the user to write serializers. It is possible, though, to
  pass type information for individual generic function call. Such a change can be applied to all the function, but it  
  will increase the network load.
* Remote functions that throw exceptions pass stack trace as a string. Because of that original stack trace on the
  client cannot be restored. To fix it, changes inside the Kotlin compiler should be made, which is complicated.
* Parameters with default values are passed on the network even if they have default values. Because we know the
  function signature statically, it can be fixed.
* Remote functions does not preserve coroutine context. All the suspend calls inside remote functions are executed
  in Ktor coroutine context. It basically means that user should provide correct coroutine context inside remote
  function bodies with suspend calls.

### Used technologies

* Kotlin multiplatform
* Kotlin compiler plugins
* Kotlinx serialization
* Kotlin Ktor

[Not updated proposal](https://docs.google.com/document/d/128EKwvuhGH6ZR8HTmQvixLaYfMQq2nwm7kQZ1i4RQSc/edit?usp=sharing)  
[Not updated presentation](https://docs.google.com/presentation/d/1IZ0D4VAie_OU0kUIY3lDtlDDSp9s02DN/edit?slide=id.g2cf53e53948_0_31#slide=id.g2cf53e53948_0_31)
