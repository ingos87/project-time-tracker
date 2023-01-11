package its.time.tracker.service.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAll
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

    "toValidMonth returns same string for valid date" {
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
})
