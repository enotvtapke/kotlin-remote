package sample
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

fun main() {
//    println()
    val myTest = create<MyTest>()
    myTest.print()
}