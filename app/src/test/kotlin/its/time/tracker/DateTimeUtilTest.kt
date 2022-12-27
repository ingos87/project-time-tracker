package its.time.tracker

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class DateTimeUtilTest : FreeSpec({

    "isValidTime is true for ..." - {
        listOf(
            "0000",
            "0730",
        ).forEach {
            "$it should be a valid time" {
                DateTimeUtil.isValidTime(it) shouldBe true
            }
        }
    }

    "isValidTime is false for ..." - {
        listOf(
            "1",
            "000",
            "930",
            "2400",
            "1060",
            "2573",
        ).forEach {
            "$it should not be a valid time" {
                DateTimeUtil.isValidTime(it) shouldBe false
            }
        }
    }

    "isValidDate is true for ..." - {
        listOf(
            "20221224",
            "20560101",
            "20240229",
            "12341231",
        ).forEach {
            "$it should be a valid date" {
                DateTimeUtil.isValidDate(it) shouldBe true
            }
        }
    }

    "isValidDate is false for ..." - {
        listOf(
            "1",
            "12",
            "123",
            "1234",
            "12345",
            "123456",
            "1234567",
            "20240230",
            "20200001",
            "20201301",
            "20200132",
            "202001311",
            ).forEach {
            "$it should not be a valid date" {
                DateTimeUtil.isValidDate(it) shouldBe false
            }
        }
    }
})
