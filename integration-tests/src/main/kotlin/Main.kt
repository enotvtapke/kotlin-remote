package sample

import examples.basic.ServerConfig
import kotlinx.remote.Remote
import kotlinx.remote.RemoteContext
import kotlinx.remote.network.RemoteCall
import kotlinx.remote.network.call

fun hello(): String = "Hello from 1"

class _MyTestProvider : MyTest{
    override fun print(){
        println("Hello from _MyTestProvider")
    }
}

interface MyTest{
    fun print()
}

fun <T> create(myTestProvider: MyTest? = null): MyTest {
    return myTestProvider!!
}

@Remote(ServerConfig::class)
context(ctx: RemoteContext)
suspend fun multiply(lhs: Long, rhs: Long) = lhs * rhs

suspend fun main() {
    context(object : RemoteContext {}) {
        println(multiply(100, 600))
    }
//    println()
    val myTest = create<MyTest>()
    myTest.print()
}