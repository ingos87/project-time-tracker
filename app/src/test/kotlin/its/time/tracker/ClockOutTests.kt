package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ClockOutTests : FunSpec({

    test("clock-out is saved with current time") {
        val output = tapSystemOut {
            main(arrayOf<String>("-f"))
        }

        output shouldStartWith "clock-out saved: 20"
        output.length shouldBeExactly 31
    }

    test("clock-out is saved with manual time") {
        val output = tapSystemOut {
            main(arrayOf<String>("--feierabend", "20221223_1730"))
        }

        output shouldBe "clock-out saved: 20221223_1730\n"
    }

    test("clock-out is saved with today's date if only time is given") {
        val output = tapSystemOut {
            main(arrayOf<String>("--feierabend", "1645"))
        }

        val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            .withZone(ZoneId.systemDefault())
        val today = formatter.format(Instant.now())

        output shouldBe "clock-out saved: ${today}_1645\n"
    }
})