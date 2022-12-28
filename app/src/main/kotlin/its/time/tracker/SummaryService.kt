package its.time.tracker

class SummaryService(
    private val verbose: Boolean,
    private val csvPath: String,
) {

    fun showDailySummary(date: String) {
        val csvService = CsvService(verbose, csvPath)
        val clockEvents = csvService.loadClockEvents()

        val daysEvents = clockEvents.filter { it.dateTime.startsWith(date) }.toList()

        val workTimeResult = WorkTimeCalculator().calculateWorkTime(daysEvents)

        println("+-------------------------------------+")
        println("| summary for day $date            |")
        println("| clock-in  ${workTimeResult.firstClockIn}                      |")
        println("| clock-out ${workTimeResult.lastClockOut}                      |")
        println("______________")
        println("| total work time  ${workTimeResult.totalWorkTime}               |")
        println("| total break time ${workTimeResult.totalBreakTime}               |")
        println("+-------------------------------------+")
    }
}