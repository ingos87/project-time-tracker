package its.time.tracker.service.util

class WorkTimeCalculator {

    fun calculateWorkTime(clockEvents: List<ClockEvent>): WorkTimeResult {
        var firstClockIn = ""
        var totalWorkTime = "0000"
        var totalBreakTime = "0000"

        var mostRecentClockIn = ""
        var mostRecentClockOut = ""

        var currentClockStatus = EventType.CLOCK_OUT

        clockEvents.forEach {
            if (it.eventType == EventType.CLOCK_IN) {
                if (firstClockIn == "") {
                    firstClockIn = it.dateTime
                    mostRecentClockIn = it.dateTime
                }
                else if (currentClockStatus == EventType.CLOCK_OUT) {
                    val breakTime = DateTimeUtil.getTimeDiff(
                        extractTime(mostRecentClockOut),
                        extractTime(it.dateTime)
                    )
                    totalBreakTime = DateTimeUtil.addTimes(totalBreakTime, breakTime)
                    mostRecentClockIn = it.dateTime
                }

                currentClockStatus = EventType.CLOCK_IN
            }
            else if (it.eventType == EventType.CLOCK_OUT) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val workTime = DateTimeUtil.getTimeDiff(
                        extractTime(mostRecentClockIn),
                        extractTime(it.dateTime)
                    )
                    totalWorkTime = DateTimeUtil.addTimes(totalWorkTime, workTime)
                    mostRecentClockOut = it.dateTime
                }

                currentClockStatus = EventType.CLOCK_OUT
            }
        }

        if (currentClockStatus != EventType.CLOCK_OUT) {
            println("No final clock-out found")
        }

        return WorkTimeResult(extractTime(firstClockIn), extractTime(mostRecentClockOut), totalWorkTime, totalBreakTime)
    }

    companion object {
        fun extractTime(dateTime: String): String {
            return dateTime.split("_")[1]
        }
    }
}

data class WorkTimeResult(
    val firstClockIn: String,
    val lastClockOut: String,
    val totalWorkTime: String,
    val totalBreakTime: String,
)