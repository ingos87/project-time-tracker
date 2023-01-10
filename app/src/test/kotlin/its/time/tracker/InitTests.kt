package its.time.tracker

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import its.time.tracker.service.AbortException
import its.time.tracker.service.ConfigService
import java.io.File

const val CFG_PATH: String = "/tmp/its-time-tracker/unittestcfg.json"

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
            executeInitWitArgs(arrayOf<String>(
                "--configpath=$CFG_PATH",
                "--csvpath=/tmp/its-time-tracker/unittesttimes.csv",
                "--myselfhr=https://blubb.de",
                "--etime=https://blah.de",
                "--weekdaysoff=MON,SAT,SUN"))
        }

        output shouldBe "Successfully created config file: /tmp/its-time-tracker/unittestcfg.json\n"

        val reader = File(CFG_PATH).inputStream().bufferedReader()
        val cfgContent = Parser.default().parse(reader) as JsonObject
        
        cfgContent[ConfigService.KEY_CSV_PATH] shouldBe "/tmp/its-time-tracker/unittesttimes.csv"
        cfgContent[ConfigService.KEY_E_TIME_URL] shouldBe "https://blah.de"
        cfgContent[ConfigService.KEY_MY_HR_SELF_SERVICE_URL] shouldBe "https://blubb.de"
        cfgContent[ConfigService.KEY_WEEKDAYS_OFF] shouldBe "MON,SAT,SUN"
        cfgContent["quark"] shouldBe null
    }

})

