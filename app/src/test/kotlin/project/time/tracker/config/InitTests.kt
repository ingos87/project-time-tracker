package project.time.tracker.config

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import project.time.tracker.executeInitWitArgs
import java.io.File
import java.util.*

const val CFG_PATH: String = "/tmp/project-time-tracker/unittestcfg.json"

class InitTests : FunSpec({

    beforeEach {
        if (File(CFG_PATH).exists()) {
            File(CFG_PATH).delete()
        }
    }

    afterEach {
        if (File(CFG_PATH).exists()) {
            File(CFG_PATH).delete()
        }
    }

    test("init creates config file, which can be read from") {
        val output = tapSystemOut {
            executeInitWitArgs(arrayOf(
                "--configpath=$CFG_PATH",
                "--csvpath=/tmp/project-time-tracker/unittesttimes.csv",
                "--myselfhr=https://blubb.de",
                "--etime=https://blah.de",
                "--weekdaysoff=MONDAY,SATURDAY,SUNDAY",
                "--daysoff=2023-05-01,2024-12-12"))
        }

        output shouldBe "Successfully created config file: /tmp/project-time-tracker/unittestcfg.json\n"

        val reader = File(CFG_PATH).inputStream().bufferedReader()
        val cfgContent = Parser.default().parse(reader) as JsonObject
        
        cfgContent[Constants::CSV_PATH.name.lowercase(Locale.getDefault())] shouldBe "/tmp/project-time-tracker/unittesttimes.csv"
        cfgContent[Constants::E_TIME_URL.name.lowercase(Locale.getDefault())] shouldBe "https://blah.de"
        cfgContent[Constants::MY_HR_SELF_SERVICE_URL.name.lowercase(Locale.getDefault())] shouldBe "https://blubb.de"
        cfgContent[Constants::WEEKDAYS_OFF.name.lowercase(Locale.getDefault())] shouldBe "MONDAY,SATURDAY,SUNDAY"
        cfgContent[Constants::DAYS_OFF.name.lowercase(Locale.getDefault())] shouldBe listOf("2023-05-01", "2024-12-12")
        cfgContent["quark"] shouldBe null
    }

})

