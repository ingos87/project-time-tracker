package its.time.tracker

import com.github.stefanbirkner.systemlambda.SystemLambda.tapSystemOut
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AuxiliaryTests : FunSpec({

    test("getVersion") {
        val output = tapSystemOut {
            main(arrayOf("version"))
        }

        output shouldBe "ITS TimeTracker App:: 0.0.1\n"
    }
})