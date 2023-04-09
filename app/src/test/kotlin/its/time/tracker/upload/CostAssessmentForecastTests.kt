package its.time.tracker.upload

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import its.time.tracker.*
import its.time.tracker.domain.CostAssessmentPosition
import java.time.Duration
import java.time.LocalDate

class CostAssessmentForecastTests : FunSpec({

    beforeEach {
        ensureTestConfig("", "", "")
        ensureCsvEmpty()
    }

    test("cost assessment works with forecast") {
        executeClockInWitArgs(arrayOf("-pWartung", "-tcoww",     "--datetime=2023-02-15 07:30"))
        executeClockInWitArgs(arrayOf("-project=\"ITS meetings\"", "-ttownhalll","--datetime=2023-02-15 09:30"))
        executeClockInWitArgs(arrayOf("-pProjectA", "-tEPP-007",  "--datetime=2023-02-15 13:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-02-15 16:30"))

        executeClockInWitArgs(arrayOf("-pWartung", "-tcoww",     "--datetime=2023-02-16 07:30"))
        executeClockInWitArgs(arrayOf("-pProjectA", "-tEPP-009",  "--datetime=2023-02-16 09:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-02-16 16:30"))

        executeClockInWitArgs(arrayOf("-pWartung", "-tcoww",     "--datetime=2023-02-17 07:30"))
        executeClockInWitArgs(arrayOf("-pProjectA", "-tEPP-007",  "--datetime=2023-02-17 09:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-02-17 16:30"))

        executeClockInWitArgs(arrayOf("-pWartung", "-tcoww",     "--datetime=2023-02-20 07:30"))
        executeClockInWitArgs(arrayOf("-pProjectA", "-tEPP-007",  "--datetime=2023-02-20 09:30"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-02-20 16:30"))

        executeClockInWitArgs(arrayOf("-pWartung", "-tcoww",     "--datetime=2023-02-21 07:30"))
        executeClockInWitArgs(arrayOf("-pProjectA", "-tEPP-009",  "--datetime=2023-02-21 08:55"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-02-21 16:30"))

        executeClockInWitArgs(arrayOf("-project=\"ITS meetings\"", "-tf2ff",     "--datetime=2023-02-22 07:30"))
        executeClockInWitArgs(arrayOf("-project=\"ITS meetings\"", "-ttownhalll","--datetime=2023-02-22 09:00"))
        executeClockInWitArgs(arrayOf("-pProjectA", "-tEPP-007",  "--datetime=2023-02-22 11:00"))
        executeClockOutWitArgs(arrayOf(             "--datetime=2023-02-22 16:30"))

        val input = mapOf<LocalDate, List<CostAssessmentPosition>>(
            LocalDate.parse("2023-02-20") to listOf(
                CostAssessmentPosition("Wartung", Duration.parse("PT2H"), emptySet()),
                CostAssessmentPosition("ProjectA", Duration.parse("PT7H"), emptySet()),
            ),
            LocalDate.parse("2023-02-21") to listOf(
                CostAssessmentPosition("Wartung", Duration.parse("PT1H25M"), emptySet()),
                CostAssessmentPosition("ProjectB", Duration.parse("PT7H35M"), emptySet()),
            ),
            LocalDate.parse("2023-02-22") to listOf(
                CostAssessmentPosition("ITS meetings", Duration.parse("PT3H30M"), emptySet()),
                CostAssessmentPosition("ProjectA", Duration.parse("PT5H32M"), emptySet()),
            ),
        )

        val days = listOf("2023-02-20", "2023-02-21", "2023-02-22", "2023-02-23", "2023-02-24", "2023-02-25", "2023-02-26")
        val output = CostAssessmentForecastService().applyForecast(days.map { LocalDate.parse(it)}.toSortedSet(), input.toSortedMap())

        output.keys.size shouldBe 5
        output.keys.last() shouldBe LocalDate.parse("2023-02-24")

        output[LocalDate.parse("2023-02-23")] shouldBe listOf(
            CostAssessmentPosition("ProjectA", Duration.parse("PT6H30M"), emptySet()),
            CostAssessmentPosition("Wartung", Duration.parse("PT1H30M"), emptySet()),
            // TODO assert ITS meetings
        )
        output[LocalDate.parse("2023-02-23")] shouldBe output[LocalDate.parse("2023-02-24")]

        // TODO rework forcast
    }
})

