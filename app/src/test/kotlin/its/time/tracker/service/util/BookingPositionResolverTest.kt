package its.time.tracker.service.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe

class BookingPositionResolverTest : StringSpec({

    "getTimeDiff works for ..." {
        listOf(
            "EPP-007" to "ProjectA",
            "epp-008" to "ProjectA",
            "EPP-009" to "ProjectB",
            "CoW" to "DoD",
            "allhands" to "ITS meetings",
            "JourFixe" to "ITS meetings",
            "EDF-" to "EDF-",
            "EDF-1234" to "EDF-1234",
            "EDF-2022" to "EDF-2022",
            "DVR-" to "Recruiting",
            "DVR-7" to "Recruiting",
            "DVR-42" to "Recruiting",
        ).forAll { (topic, expectedBookingPosition) ->
            BookingPositionResolver.resolveTopicToBookingPosition(topic) shouldBe expectedBookingPosition
        }
    }
})
