package its.time.tracker

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ScenarioTests : BehaviorSpec({

    given("standard work day") {
        ensureTestConfig()
        ensureCsvEmpty()
        getTimesCsvContent() shouldBe emptyList()
        `when`("clock-in is triggered") {
            executeClockInWitArgs(arrayOf("-tEPP-007", "--datetime=2022-01-03 07:30"))
            then("event was inserted into csv") {
                getTimesCsvContent() shouldBe listOf(
                    "dateTime;eventType;topic",
                    "2022-01-03 07:30;CLOCK_IN;EPP-007")
            }
        }

        `when`("another clock-in is triggered") {
            executeClockInWitArgs(arrayOf("--topic=EPP-123", "--datetime=2022-01-03 10:00"))
            then("event was inserted into csv") {
                getTimesCsvContent() shouldBe listOf(
                    "dateTime;eventType;topic",
                    "2022-01-03 07:30;CLOCK_IN;EPP-007",
                    "2022-01-03 10:00;CLOCK_IN;EPP-123")
            }
        }

        `when`("clock-out is triggered") {
            executeClockOutWitArgs(arrayOf("-d2022-01-03 17:00"))
            then("event was inserted into csv") {
                getTimesCsvContent() shouldBe listOf(
                    "dateTime;eventType;topic",
                    "2022-01-03 07:30;CLOCK_IN;EPP-007",
                    "2022-01-03 10:00;CLOCK_IN;EPP-123",
                    "2022-01-03 17:00;CLOCK_OUT;MANUAL_CLOCK_OUT")
            }
        }
    }
})

