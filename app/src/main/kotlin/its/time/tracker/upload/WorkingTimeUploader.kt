package its.time.tracker.upload

import its.time.tracker.domain.WorkDaySummary
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.SortedMap

class WorkingTimeUploader(private val workingTimesByDay: SortedMap<LocalDate, WorkDaySummary>) {

    fun submit() {
        navigateToTimeCorrectionPage()
        workingTimesByDay.forEach{ entry ->
            if (isValidInput(entry)) {
                navigateToDay(entry.key)
                ensureClockInClockOutPresent(entry.value)
            }
        }
    }

    private fun navigateToTimeCorrectionPage() {
        //TODO("Not yet implemented")
    }

    private fun ensureClockInClockOutPresent(value: WorkDaySummary?) {
        //TODO("Not yet implemented")
    }

    private fun isValidInput(entry: Map.Entry<LocalDate, WorkDaySummary>): Boolean {
        val entryAge = ChronoUnit.DAYS.between(entry.key, LocalDate.now())
        return entryAge <= 30L
    }

    private fun navigateToDay(key: LocalDate) {

    }

}