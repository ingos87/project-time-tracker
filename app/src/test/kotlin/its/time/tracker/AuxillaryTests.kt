package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AuxillaryTests : FunSpec({

    test("getVersion") {
        val output = tapSystemOut {
            main(arrayOf<String>("-v"))
        }

        output shouldBe "ITS TimeTracker App:: 0.0.1\n"
    }

    test("getConfig") {
        val output = tapSystemOut {
            main(arrayOf<String>("-c"))
        }

        output shouldBe "some day, I will list the config params here\n"
    }

    test("getVersion will overwrite getConfig") {
        val output = tapSystemOut {
            main(arrayOf<String>("-v", "-c"))
        }

        output shouldBe "ITS TimeTracker App:: 0.0.1\n"
    }
})