package mymandelbrot

class CentralNode {
    val workers = mutableListOf<Worker>()
    fun register(node: Worker) {
        workers.add(node)
    }
}