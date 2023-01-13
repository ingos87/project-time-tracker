package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

class UploadWorkingTimeTests : FunSpec({

    beforeEach {
        ensureTestConfig()
        ensureCsvEmpty()
    }

    test("recording working time is not possible if there is no config file") {
        ensureNoConfig()

        val output = tapSystemOut {
            main(arrayOf<String>("upload-working-time"))
        }

        output shouldStartWith "No config file found in ./app.json\n" +
                "Use 'java -jar app.jar init' with the according parameters"
    }

    test("todo: name test") {

        val output = tapSystemOut {
            executeUploadWorkingTimeWitArgs(arrayOf<String>("-w2023-34"))
        }

        splitIgnoreBlank(output) shouldBe listOf(
            "done")
    }

})
