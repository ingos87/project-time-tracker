package its.time.tracker.service

import its.time.tracker.domain.WorkDaySummary
import its.time.tracker.upload.WorkingTimeNormalizer
import its.time.tracker.upload.WorkingTimeUploader
import its.time.tracker.util.ClockEventsFilter
import its.time.tracker.util.DATE_PATTERN
import its.time.tracker.util.DateTimeUtil
import its.time.tracker.util.TIME_PATTERN
import java.time.LocalDate

class WorkingTimeService {
    fun captureWorkingTime(localDate: LocalDate, granularity: Granularity, noop: Boolean) {
        val csvService = CsvService()
        val clockEvents = csvService.loadClockEvents()

        val allDays: List<LocalDate> = when(granularity) {
            Granularity.MONTH -> DateTimeUtil.getAllDaysInSameMonthAs(localDate)
            else -> DateTimeUtil.getAllDaysInSameWeekAs(localDate)
        }

        val workingTimeNormalizer = WorkingTimeNormalizer()
        val workingTimeResults = HashMap<LocalDate, WorkDaySummary>()
        allDays.forEach{ date ->
            val workDaySummary = WorkDaySummary.toWorkDaySummary(ClockEventsFilter.getEventsBelongingToSameDay(clockEvents, date))
            if (workDaySummary != null) {
                workingTimeResults[date] = workDaySummary
            }
        }

        val normalizedWorkingTimes = workingTimeNormalizer.normalizeWeekWorkingTime(workingTimeResults)

        println(" date       │ compliant values    ║ original values")
        println("────────────┼─────────────┼───────╬─────────────┼───────")

        normalizedWorkingTimes.forEach{ entry ->
            val sb = StringBuilder()
            sb.append(" " + DateTimeUtil.temporalToString(entry.key, DATE_PATTERN))
            sb.append(" │")
            if (entry.value.last().clockIn == entry.value.last().clockOut) {
                sb.append("            ")
            } else {
                sb.append(" " + DateTimeUtil.temporalToString(entry.value.last().clockIn.toLocalTime(), TIME_PATTERN))
                sb.append("-" + DateTimeUtil.temporalToString(entry.value.last().clockOut.toLocalTime(), TIME_PATTERN))
            }
            sb.append(" │")
            sb.append(" " + DateTimeUtil.durationToString(entry.value.last().workDuration))
            sb.append(" ║")
            sb.append(" " + DateTimeUtil.temporalToString(entry.value.first().clockIn.toLocalTime(), TIME_PATTERN))
            sb.append("-" + DateTimeUtil.temporalToString(entry.value.first().clockOut.toLocalTime(), TIME_PATTERN))
            sb.append(" │")
            sb.append(" " + DateTimeUtil.durationToString(entry.value.first().workDuration))
            println(sb.toString())
        }

        if (noop) {
            println("\nNOOP mode. Uploaded nothing")
        } else {
            println("\nUploading clock-ins and clock-outs to myHRSelfService ...")
            val finalWorkingTimes = normalizedWorkingTimes.map { entry -> entry.key to entry.value.last() }.toMap()
            WorkingTimeUploader(finalWorkingTimes.toSortedMap()).submit()

            /*
            current failure:

            SLF4J: No SLF4J providers were found.
            SLF4J: Defaulting to no-operation (NOP) logger implementation
            SLF4J: See https://www.slf4j.org/codes.html#noProviders for further details.
            Starting ChromeDriver 109.0.5414.74 (e7c5703604daa9cc128ccf5a5d3e993513758913-refs/branch-heads/5414@{#1172}) on port 4264
            Only local connections are allowed.
            Please see https://chromedriver.chromium.org/security-considerations for suggestions on keeping ChromeDriver safe.
            ChromeDriver was started successfully.
            Jan 20, 2023 4:13:46 PM org.openqa.selenium.devtools.CdpVersionFinder findNearestMatch
            WARNING: Unable to find an exact match for CDP version 109, so returning the closest version found: 106
            Exception in thread "main" org.openqa.selenium.NoSuchElementException: no such element: Unable to locate element: {"method":"css selector","selector":"#__tile2"}
              (Session info: chrome=109.0.5414.87)
            For documentation on this error, please visit: https://selenium.dev/exceptions/#no_such_element
            Build info: version: '4.7.2', revision: '4d4020c3b7'
            System info: os.name: 'Mac OS X', os.arch: 'x86_64', os.version: '11.6', java.version: '18.0.1'
            Driver info: org.openqa.selenium.chrome.ChromeDriver
            Command: [0b62ff02316862a479d8df7e1852c2a1, findElement {using=id, value=__tile2}]
            Capabilities {acceptInsecureCerts: false, browserName: chrome, browserVersion: 109.0.5414.87, chrome: {chromedriverVersion: 109.0.5414.74 (e7c5703604da..., userDataDir: /var/folders/wr/kfnf925d49v...}, goog:chromeOptions: {debuggerAddress: localhost:63449}, networkConnectionEnabled: false, pageLoadStrategy: normal, platformName: MAC, proxy: Proxy(), se:cdp: ws://localhost:63449/devtoo..., se:cdpVersion: 109.0.5414.87, setWindowRect: true, strictFileInteractability: false, timeouts: {implicit: 0, pageLoad: 300000, script: 30000}, unhandledPromptBehavior: dismiss and notify, webauthn:extension:credBlob: true, webauthn:extension:largeBlob: true, webauthn:virtualAuthenticators: true}
            Session ID: 0b62ff02316862a479d8df7e1852c2a1
                at java.base/jdk.internal.reflect.DirectConstructorHandleAccessor.newInstance(DirectConstructorHandleAccessor.java:67)
                at java.base/java.lang.reflect.Constructor.newInstanceWithCaller(Constructor.java:499)
                at java.base/java.lang.reflect.Constructor.newInstance(Constructor.java:483)
                at org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec.createException(W3CHttpResponseCodec.java:200)
                at org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec.decode(W3CHttpResponseCodec.java:133)
                at org.openqa.selenium.remote.codec.w3c.W3CHttpResponseCodec.decode(W3CHttpResponseCodec.java:53)
                at org.openqa.selenium.remote.HttpCommandExecutor.execute(HttpCommandExecutor.java:184)
                at org.openqa.selenium.remote.service.DriverCommandExecutor.invokeExecute(DriverCommandExecutor.java:167)
                at org.openqa.selenium.remote.service.DriverCommandExecutor.execute(DriverCommandExecutor.java:142)
                at org.openqa.selenium.remote.RemoteWebDriver.execute(RemoteWebDriver.java:535)
                at org.openqa.selenium.remote.ElementLocation$ElementFinder$2.findElement(ElementLocation.java:162)
                at org.openqa.selenium.remote.ElementLocation.findElement(ElementLocation.java:66)
                at org.openqa.selenium.remote.RemoteWebDriver.findElement(RemoteWebDriver.java:351)
                at org.openqa.selenium.remote.RemoteWebDriver.findElement(RemoteWebDriver.java:343)
                at org.openqa.selenium.support.pagefactory.DefaultElementLocator.findElement(DefaultElementLocator.java:70)
                at org.openqa.selenium.support.pagefactory.internal.LocatingElementHandler.invoke(LocatingElementHandler.java:39)
                at jdk.proxy2/jdk.proxy2.$Proxy9.click(Unknown Source)
                at its.time.tracker.webpages.myhrselfservice.LandingPage.clickTimeCorrectionsTile(LandingPage.kt:18)
                at its.time.tracker.upload.WorkingTimeUploader.navigateToTimeCorrectionLandingPage(WorkingTimeUploader.kt:31)
                at its.time.tracker.upload.WorkingTimeUploader.submit(WorkingTimeUploader.kt:17)
                at its.time.tracker.service.WorkingTimeService.captureWorkingTime(WorkingTimeService.kt:61)
                at its.time.tracker.Timekeeping.run(App.kt:145)
                at com.github.ajalt.clikt.parsers.Parser.parse(Parser.kt:198)
                at com.github.ajalt.clikt.parsers.Parser.parse(Parser.kt:211)
                at com.github.ajalt.clikt.parsers.Parser.parse(Parser.kt:18)
                at com.github.ajalt.clikt.core.CliktCommand.parse(CliktCommand.kt:400)
                at com.github.ajalt.clikt.core.CliktCommand.parse$default(CliktCommand.kt:397)
                at com.github.ajalt.clikt.core.CliktCommand.main(CliktCommand.kt:415)
                at com.github.ajalt.clikt.core.CliktCommand.main(CliktCommand.kt:440)
                at its.time.tracker.AppKt.main(App.kt:161)
             */
        }
    }
}

enum class Granularity {
    WEEK, MONTH
}