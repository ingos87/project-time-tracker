package its.time.tracker.upload

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import its.time.tracker.config.Constants
import its.time.tracker.ensureTestConfig

class BookingPositionResolverTest : StringSpec({

    beforeEach {
        ensureTestConfig()
    }

    "getTimeDiff works for ..." {
        listOf(
            "EPP-007" to "ProjectA",
            "epp-008" to "ProjectA",
            "EPP-009" to "ProjectB",
            "CoWw" to "DoD",
            "allhandss" to "ITS meetings",
            "JourFixee" to "ITS meetings",
            "EDF-" to "Wartung",
            "EDF-1234" to "Wartung",
            "EDF-2022" to "Wartung",
            "DVR-" to "Line Activity",
            "DVR-7" to "Line Activity",
            "DVR-42" to "Line Activity",
            "abcdefg" to "Project Placeholder",
        ).forAll { (topic, expectedBookingPosition) ->
            Constants.COST_ASSESSMENTS.resolveTopicToProject(topic) shouldBe expectedBookingPosition
        }
    }
})
