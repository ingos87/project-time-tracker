package its.time.tracker.service.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import its.time.tracker.service.AbortException
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DateTimeUtilTests : StringSpec({

    "toValidDate returns same string for valid date" {
        listOf(
            "2022-12-01" to "2022-12-01",
            "2020-02-29" to "2020-02-29",
            "2045-01-01" to "2045-01-01",
        ).forAll { (inputDateTimeString, expectedDateTimeString) ->
            DateTimeUtil.toValidDate(inputDateTimeString).toString() shouldBe expectedDateTimeString
        }
    }

    "toValidDate returns today if input is empty" {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        formatter.format(DateTimeUtil.toValidDate("")) shouldBe today
        formatter.format(DateTimeUtil.toValidDate(null)) shouldBe today
    }

    "toValidDate throws Exception if input is invalid" {
        listOf(
            "1",
            "12",
            "2022-13-01",
            "2022-02-29",
            "0000-00-00",
            "not_a_date",
        ).forAll {
            assertThrows<AbortException> {
                DateTimeUtil.toValidDate(it)
            }
        }
    }

    "toValidDateTime returns same string for valid datetime" {
        listOf(
            "2022-12-01 10:00" to "2022-12-01T10:00",
            "2020-02-29 23:59" to "2020-02-29T23:59",
            "2045-01-01 00:00" to "2045-01-01T00:00",
        ).forAll { (inputDateTimeString, expectedDateTimeString) ->
            DateTimeUtil.toValidDateTime(inputDateTimeString).toString() shouldBe expectedDateTimeString
        }
    }

    "toValidDateTime returns now if input is empty" {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
        val now = formatter.format(Instant.now())

        formatter.format(DateTimeUtil.toValidDateTime("")) shouldBe now
        formatter.format(DateTimeUtil.toValidDateTime(null)) shouldBe now
    }

    "toValidDateTime returns now and adds date if input only time is given" {
        listOf(
            "00:00",
            "12:34",
            "23:59",
        ).forAll {
            val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                .withZone(ZoneId.systemDefault())
            val today = formatter.format(Instant.now())

            val formatter2: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault())

            formatter2.format(DateTimeUtil.toValidDateTime(it)) shouldBe "$today $it"
        }
    }

    "toValidDateTime throws Exception if input is invalid" {
        listOf(
            "1",
            "12",
            "2022-13-01 07:00",
            "2022-02-29 12:45",
            "0000-00-00 00:00",
            "not_a_date",
            "2020-01-01 23:60",
        ).forAll {
            assertThrows<AbortException> {
                DateTimeUtil.toValidDateTime(it)
            }
        }
    }

    "toValidMonth returns same value for valid date" {
        listOf(
            "2022-12" to "2022-12-01",
            "2020-02" to "2020-02-01",
            "2045-01" to "2045-01-01",
        ).forAll { (inputDateTimeString, expectedDateTimeString) ->
            DateTimeUtil.toValidMonth(inputDateTimeString).toString() shouldBe expectedDateTimeString
        }
    }

    "toValidMonth returns today if input is empty" {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        formatter.format(DateTimeUtil.toValidMonth("")) shouldBe today
        formatter.format(DateTimeUtil.toValidMonth(null)) shouldBe today
    }

    "toValidMonth throws Exception if input is invalid" {
        listOf(
            "1",
            "12",
            "2022-13-01",
            "2022-02-29",
            "0000-00-00",
            "2000-00-15",
            "not_a_date",
        ).forAll {
            assertThrows<AbortException> {
                DateTimeUtil.toValidMonth(it)
            }
        }
    }

    "toValidCalendarWeek returns specific date for input week-of-year" {
        listOf(
            "2023-01" to "2023-01-02",
            "2023-02" to "2023-01-09",
            "2023-20" to "2023-05-15",
            "2023-50" to "2023-12-11",
            "2023-52" to "2023-12-25",
            "2024-52" to "2024-12-23",
            "2025-01" to "2024-12-30",
            "2031-01" to "2030-12-30",
            "2031-20" to "2031-05-12",
            "2023-53" to "2024-01-01", // there is no week 53 in 2023
        ).forAll { (inputWeekOfYearString, expectedDateString) ->
            DateTimeUtil.toValidCalendarWeek(inputWeekOfYearString).toString() shouldBe expectedDateString
        }
    }

    "toValidCalendarWeek returns today if input is empty" {
        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        formatter.format(DateTimeUtil.toValidCalendarWeek("")) shouldBe today
        formatter.format(DateTimeUtil.toValidCalendarWeek(null)) shouldBe today
    }

    "toValidCalendarWeek throws exception for invalid input" {
        listOf(
            "2023-1",
            "2023",
            "2023-007",
            "2023-00",
            "2023-54",
            "bullshit",
            "01-2022",
            "-100-01",
        ).forAll {
            assertThrows<AbortException> {
                DateTimeUtil.toValidCalendarWeek(it)
            }
        }
    }

    "getWeekOfYearFromDate works for valid dates" {
        listOf(
            LocalDate.parse("2023-01-01") to "52",
            LocalDate.parse("2023-01-02") to "01",
            LocalDate.parse("2027-01-01") to "53",
            LocalDate.parse("2019-02-07") to "06",
            LocalDate.parse("2024-01-01") to "01",
        ).forAll { (inputDate, expectedWeekOfYear) ->
            DateTimeUtil.getWeekOfYearFromDate(inputDate) shouldBe expectedWeekOfYear
        }
    }

    "isSameDay works for ..." {
        val dtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
        val dFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withZone(ZoneId.systemDefault())
        listOf(
            Pair(
                LocalDateTime.parse("2020-12-01 10:00", dtFormatter),
                LocalDate.parse("2020-12-01", dFormatter)) to true,
            Pair(
                LocalDateTime.parse("2020-12-05 10:00", dtFormatter),
                LocalDate.parse("2020-12-01", dFormatter)) to false,
        ).forAll { (input, expectedResult) ->
            DateTimeUtil.isSameDay(input.first, input.second) shouldBe expectedResult
        }
    }

    "isSameMonth works for ..." {
        val dtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
        val dFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withZone(ZoneId.systemDefault())
        listOf(
            Pair(
                LocalDateTime.parse("2020-12-01 10:00", dtFormatter),
                LocalDate.parse("2020-12-05", dFormatter)) to true,
            Pair(
                LocalDateTime.parse("2020-12-01 10:00", dtFormatter),
                LocalDate.parse("2020-12-31", dFormatter)) to true,
            Pair(
                LocalDateTime.parse("2020-12-01 10:00", dtFormatter),
                LocalDate.parse("2020-12-01", dFormatter)) to true,
            Pair(
                LocalDateTime.parse("2020-12-01 10:00", dtFormatter),
                LocalDate.parse("2020-11-01", dFormatter)) to false,
            Pair(
                LocalDateTime.parse("2020-12-01 10:00", dtFormatter),
                LocalDate.parse("2020-01-01", dFormatter)) to false,
        ).forAll { (input, expectedResult) ->
            DateTimeUtil.isSameMonth(input.first, input.second) shouldBe expectedResult
        }
    }

    "dateTimeToString works for ..." {
        val dtFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault())
        val dFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withZone(ZoneId.systemDefault())
        listOf(
            Pair(LocalDateTime.parse("2022-12-01 10:00", dtFormatter), DATE_TIME_PATTERN) to "2022-12-01 10:00",
            Pair(LocalDateTime.parse("2021-01-15 23:59", dtFormatter), DATE_TIME_PATTERN) to "2021-01-15 23:59",
            Pair(LocalDate.parse("2022-12-01", dFormatter), DATE_PATTERN) to "2022-12-01",
            Pair(LocalDate.parse("1987-03-12", dFormatter), DATE_PATTERN) to "1987-03-12",
        ).forAll { (params, expectedResult) ->
            DateTimeUtil.temporalToString(params.first, params.second) shouldBe expectedResult
        }
    }

    "durationToString works for ..." {
        listOf(
            Duration.ofHours(3L) to "03:00",
            Duration.ofMinutes(90L) to "01:30",
            Duration.ofMinutes(502L) to "08:22",
            Duration.ofHours(50L) to "50:00",
            Duration.ofHours(250L) to "250:00",
        ).forAll { (duration, expectedResult) ->
            DateTimeUtil.durationToString(duration) shouldBe expectedResult
        }
    }

    "getAllDaysInSameWeekAs works for ..." {
        listOf(
            LocalDate.parse("2023-01-09") to listOf(
                LocalDate.parse("2023-01-09"),
                LocalDate.parse("2023-01-15"),
            ),
            LocalDate.parse("2023-01-10") to listOf(
                LocalDate.parse("2023-01-09"),
                LocalDate.parse("2023-01-15"),
            ),
            LocalDate.parse("2023-01-15") to listOf(
                LocalDate.parse("2023-01-09"),
                LocalDate.parse("2023-01-15"),
            ),
        ).forAll { (date, firstAndLastDay) ->
            val result = DateTimeUtil.getAllDaysInSameWeekAs(date)
            result.size shouldBe 7
            result.first() shouldBe firstAndLastDay.first()
            result.last() shouldBe firstAndLastDay.last()
        }
    }

    "getAllDaysInSameMonthAs works for ..." {
        listOf(
            LocalDate.parse("2023-04-09") to listOf(
                LocalDate.parse("2023-04-01"),
                LocalDate.parse("2023-04-30"),
            ),
            LocalDate.parse("2024-02-10") to listOf(
                LocalDate.parse("2024-02-01"),
                LocalDate.parse("2024-02-29"),
            ),
            LocalDate.parse("2021-12-15") to listOf(
                LocalDate.parse("2021-12-01"),
                LocalDate.parse("2021-12-31"),
            ),
        ).forAll { (date, firstAndLastDay) ->
            val result = DateTimeUtil.getAllDaysInSameMonthAs(date)
            result.size shouldBeGreaterThanOrEqual 28
            result.first() shouldBe firstAndLastDay.first()
            result.last() shouldBe firstAndLastDay.last()
        }
    }
})
