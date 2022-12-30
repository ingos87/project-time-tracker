package its.time.tracker.service.util

import its.time.tracker.service.util.DateTimeUtil.Companion.addTimes
import its.time.tracker.service.util.DateTimeUtil.Companion.extractTimeFromDateTime
import its.time.tracker.service.util.DateTimeUtil.Companion.getTimeDiff

class WorkTimeCalculator {

    companion object {
        private const val MAX_WORK_HOURS_PER_DAY = "0900"
    }

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
                    val breakTime = getTimeDiff(
                        extractTimeFromDateTime(mostRecentClockOut),
                        extractTimeFromDateTime(it.dateTime)
                    )
                    totalBreakTime = addTimes(totalBreakTime, breakTime)
                    mostRecentClockIn = it.dateTime
                }

                currentClockStatus = EventType.CLOCK_IN
            }
            else if (it.eventType == EventType.CLOCK_OUT) {
                if (currentClockStatus == EventType.CLOCK_IN) {
                    val workTime = getTimeDiff(
                        extractTimeFromDateTime(mostRecentClockIn),
                        extractTimeFromDateTime(it.dateTime)
                    )
                    totalWorkTime = addTimes(totalWorkTime, workTime)
                    mostRecentClockOut = it.dateTime
                }

                currentClockStatus = EventType.CLOCK_OUT
            }
        }

        if (currentClockStatus != EventType.CLOCK_OUT) {
            if (totalWorkTime.toInt() >= MAX_WORK_HOURS_PER_DAY.toInt()) {
                val thirtyMinutes = "0030" // although, this is beyond the max hours per day, any new tasks will take at least half an hour

                totalWorkTime = addTimes(totalWorkTime, thirtyMinutes)
                val mostRecentClockInSplit = mostRecentClockIn.split("_")
                val newEndTime = addTimes(mostRecentClockInSplit[1], thirtyMinutes)
                mostRecentClockOut = "${mostRecentClockInSplit[0]}_$newEndTime"
            }
            else {
                val timeTillMax = getTimeDiff(totalWorkTime, MAX_WORK_HOURS_PER_DAY)

                totalWorkTime = MAX_WORK_HOURS_PER_DAY
                mostRecentClockOut = addTimes(mostRecentClockIn, timeTillMax)
            }
            println("No final clock-out found. Will insert one to fill up working time to $totalWorkTime hours.")
        }

        return WorkTimeResult(
            firstClockIn = extractTimeFromDateTime(firstClockIn),
            lastClockOut = extractTimeFromDateTime(mostRecentClockOut),
            totalWorkTime = totalWorkTime,
            totalBreakTime = totalBreakTime)
    }
}

data class WorkTimeResult(
    val firstClockIn: String,
    val lastClockOut: String,
    val totalWorkTime: String,
    val totalBreakTime: String,
)