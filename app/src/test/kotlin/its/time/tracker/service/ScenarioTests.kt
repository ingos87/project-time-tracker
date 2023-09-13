package its.time.tracker.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import its.time.tracker.*

class ScenarioTests : BehaviorSpec({

    given("standard work day") {
        ensureTestConfig("", "", "")
        ensureCsvEmpty()
        getTimesCsvContent() shouldBe emptyList()
        `when`("clock-in is triggered") {
            executeClockInWitArgs(arrayOf("-pwartung", "-tcode", "-sEPP-007", "--datetime=2022-01-03 07:30"))
            then("event was inserted into csv") {
                getTimesCsvContent() shouldBe listOf(
                    "dateTime;eventType;project;topic;story",
                    "2022-01-03 07:30;CLOCK_IN;wartung;code;EPP-007")
            }
        }

        `when`("another clock-in is triggered") {
            executeClockInWitArgs(arrayOf("--project=wartung", "--topic=meeting", "--storyEPP-008", "--datetime=2022-01-03 10:00"))
            then("event was inserted into csv") {
                getTimesCsvContent() shouldBe listOf(
                    "dateTime;eventType;project;topic;story",
                    "2022-01-03 07:30;CLOCK_IN;wartung;code;EPP-007",
                    "2022-01-03 10:00;CLOCK_IN;wartung;meeting;EPP-008")
            }
        }

        `when`("clock-out is triggered") {
            executeClockOutWitArgs(arrayOf("-d2022-01-03 17:00"))
            then("event was inserted into csv") {
                getTimesCsvContent() shouldBe listOf(
                    "dateTime;eventType;project;topic;story",
                    "2022-01-03 07:30;CLOCK_IN;wartung;EPP-007;",
                    "2022-01-03 10:00;CLOCK_IN;wartung;EPP-123;",
                    "2022-01-03 17:00;CLOCK_OUT;;MANUAL_CLOCK_OUT;")
            }
        }
    }
})

