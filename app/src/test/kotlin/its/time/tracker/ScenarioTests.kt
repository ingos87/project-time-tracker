package its.time.tracker

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe

class ScenarioTests : BehaviorSpec({

    given("standard work day") {
        ensureCsvEmpty()
        getTimesCsvContent() shouldBe emptyList()
        `when`("clock-in is triggered") {
            executeClockInWitArgs(arrayOf<String>("-tEPP-007", "--datetime=20220103_0730"))
            then("event was inserted into csv") {
                getTimesCsvContent() shouldBe listOf(
                    "dateTime;eventType;topic",
                    "20220103_0730;CLOCK_IN;EPP-007")
            }
        }

        `when`("another clock-in is triggered") {
            executeClockInWitArgs(arrayOf<String>("--topic=EPP-123", "--datetime=20220103_1000"))
            then("event was inserted into csv") {
                getTimesCsvContent() shouldBe listOf(
                    "dateTime;eventType;topic",
                    "20220103_0730;CLOCK_IN;EPP-007",
                    "20220103_1000;CLOCK_IN;EPP-123")
            }
        }

        `when`("clock-out is triggered") {
            executeClockOutWitArgs(arrayOf<String>("-d20220103_1700"))
            then("event was inserted into csv") {
                getTimesCsvContent() shouldBe listOf(
                    "dateTime;eventType;topic",
                    "20220103_0730;CLOCK_IN;EPP-007",
                    "20220103_1000;CLOCK_IN;EPP-123",
                    "20220103_1700;CLOCK_OUT;MANUAL_CLOCK_OUT")
            }
        }
    }
})

