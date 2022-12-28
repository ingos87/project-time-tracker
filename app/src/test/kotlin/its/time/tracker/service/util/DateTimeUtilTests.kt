package its.time.tracker.service.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe

class DateTimeUtilTests : StringSpec({

    "isValidTime is true for ..." {
        listOf(
            "0000",
            "0730",
        ).forEach {
            DateTimeUtil.isValidTime(it) shouldBe true
        }
    }

    "isValidTime is false for ..." {
        listOf(
            "1",
            "000",
            "930",
            "2400",
            "1060",
            "2573",
        ).forEach {
            DateTimeUtil.isValidTime(it) shouldBe false
        }
    }

    "isValidDate is true for ..." {
        listOf(
            "20221224",
            "20560101",
            "20240229",
            "12341231",
        ).forEach {
            DateTimeUtil.isValidDate(it) shouldBe true
        }
    }

    "isValidDate is false for ..." {
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
            DateTimeUtil.isValidDate(it) shouldBe false
        }
    }

    "addTimes works for ..." {
        listOf(
            Pair("0000", "0000") to "0000",
            Pair("0000", "0010") to "0010",
            Pair("1005", "1010") to "2015",
            Pair("1555", "0110") to "1705",
            Pair("2359", "0001") to "2400",
            Pair("2330", "0245") to "2615",
        ).forAll { (times, expectedSum) ->
            DateTimeUtil.addTimes(times.first, times.second) shouldBe expectedSum
        }
    }

    "getTimeDiff works for ..." {
        listOf(
            Pair("0000", "0000") to "0000",
            Pair("0000", "0010") to "0010",
            Pair("1005", "1010") to "0005",
            Pair("0730", "0845") to "0115",
            Pair("0745", "0830") to "0045",
            Pair("0800", "2200") to "1400",
            Pair("2200", "0100") to "0300",
            Pair("2230", "0100") to "0230",
        ).forAll { (times, expectedDiff) ->
            DateTimeUtil.getTimeDiff(times.first, times.second) shouldBe expectedDiff
        }
    }
})
