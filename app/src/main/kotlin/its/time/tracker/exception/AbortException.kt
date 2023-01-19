package its.time.tracker.exception

class AbortException(message: String, private var details: List<String> = emptyList()) : RuntimeException(message) {

    fun printMessage() {
        println(message)
        details.forEach {
            println(it)
        }
    }
}