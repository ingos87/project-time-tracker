package its.time.tracker

import its.time.tracker.App.printVersion

object App {
    const val appName = "ITS TimeTracker App"
    const val version = "0.0.1"
    fun printVersion() {
        println("${appName}:: ${version}")
    }
}
fun main(args: Array<String>) {
    printVersion()
}
