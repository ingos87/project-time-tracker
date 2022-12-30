package its.time.tracker.service.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DateTimeUtilTests : StringSpec({

    "isValidTime is true for ..." {
        listOf(
            "00:00",
            "07:30",
        ).forEach {
            DateTimeUtil.isValidTime(it) shouldBe true
        }
    }

    "isValidTime is false for ..." {
        listOf(
            "1",
            "0900",
            "0:00",
            "9:30",
            "24:00",
            "10:60",
            "25:73",
        ).forEach {
            DateTimeUtil.isValidTime(it) shouldBe false
        }
    }

    "isValidDate is true for ..." {
        listOf(
            "2022-12-24",
            "2056-01-01",
            "2024-02-29",
            "1234-12-31",
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
            "1234-5",
            "1234-56",
            "1234-56-7",
            "2024-02-30",
            "2020-00-01",
            "2020-13-01",
            "2020-01-32",
            "2020-01-311",
        ).forEach {
            DateTimeUtil.isValidDate(it) shouldBe false
        }
    }

    "extractTimeFromDateTime works for ..." {
        listOf(
            "20221224_1800" to "1800",
            "20221224_1" to "1",
            "nonesense_0930" to "0930",
        ).forAll { (dateTime, expectedTime) ->
            DateTimeUtil.extractTimeFromDateTime(dateTime) shouldBe expectedTime
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

    "toValidDate returns same string for valid date" {
        listOf(
            "20221201" to "20221201",
            "20200229" to "20200229",
            "20450101" to "20450101",
        ).forAll { (inputDateTimeString, expectedDateTimeString) ->
            DateTimeUtil.toValidDate(inputDateTimeString) shouldBe expectedDateTimeString
        }
    }

    "toValidDate returns today if input is empty" {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        DateTimeUtil.toValidDate("") shouldBe today
        DateTimeUtil.toValidDate(null) shouldBe today
    }

    "toValidDate returns null if input is invalid" {
        listOf(
            "1",
            "12",
            "20221301",
            "20220229",
            "00000000",
            "not_a_date",
        ).forAll {
            DateTimeUtil.toValidDate(it) shouldBe null
        }
    }

    "toValidDateTime returns same string for valid datetime" {
        listOf(
            "20221201_1000" to "20221201_1000",
            "20200229_2359" to "20200229_2359",
            "20450101_0000" to "20450101_0000",
        ).forAll { (inputDateTimeString, expectedDateTimeString) ->
            DateTimeUtil.toValidDateTime(inputDateTimeString) shouldBe expectedDateTimeString
        }
    }

    "toValidDateTime returns now if input is empty" {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")
            .withZone(ZoneId.systemDefault())
        val now = formatter.format(Instant.now())

        DateTimeUtil.toValidDateTime("") shouldBe now
        DateTimeUtil.toValidDateTime(null) shouldBe now
    }

    "toValidDateTime returns now and adds date if input only time is given" {
        listOf(
            "0000",
            "1234",
            "2359",
        ).forAll {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneId.systemDefault())
            val today = formatter.format(Instant.now())

            DateTimeUtil.toValidDateTime(it) shouldBe "${today}_$it"
        }
    }

    "toValidDateTime returns null if input is invalid" {
        listOf(
            "1",
            "12",
            "20221301_0700",
            "20220229_1245",
            "00000000_0000",
            "not_a_date",
            "20200101_2360",
        ).forAll {
            DateTimeUtil.toValidDateTime(it) shouldBe null
        }
    }

    "addTimeToDatetime works for ..." {
        listOf(
            Pair("20200101_1800", "0100") to "20200101_1900",
            Pair("20200101_1000", "1000") to "20200101_2000",
            Pair("20200101_2300", "0100") to "20200102_0000",
        ).forAll { (times, expectedDateTime) ->
            DateTimeUtil.addTimeToDateTime(times.first, times.second) shouldBe expectedDateTime
        }
    }
})
