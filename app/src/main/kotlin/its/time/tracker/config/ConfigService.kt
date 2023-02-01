package its.time.tracker.config

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import its.time.tracker.exception.AbortException
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class ConfigService private constructor(private var configFilePath: String) {

    companion object {
        fun createConfigService(configFilePath: String?): ConfigService {
            return ConfigService(configFilePath ?: "./app.json")
        }

        fun createConfigFileWithParams(
            configFilePath: String = "./app.json",
            csvPath: String = "/Users/me/its_times.csv",
            myHrSelfServiceUrl: String = "https://someurl.de",
            myHrSelfServiceLanguage: String = "EN",
            eTimeUrl: String = "https://someurl.de",
            eTimeLanguage: String = "EN",
            maxDailyWorkTillAutoClockOut: String = "",
            weekdaysOff: String = "",
            daysOff: String = "",
            chromeProfilePath: String = "",
        ) {
            if (File(configFilePath).exists()) {
                println("$configFilePath already exists.")
                return
            }

            val defaultConfig = listOf(
                "{",
                "  \"${Constants::CSV_PATH.name.lowercase(Locale.GERMANY)}\":\"$csvPath\",",
                "  \"${Constants::MY_HR_SELF_SERVICE_URL.name.lowercase(Locale.GERMANY)}\":\"$myHrSelfServiceUrl\",",
                "  \"${Constants::MY_HR_SELF_SERVICE_LANGUAGE.name.lowercase(Locale.GERMANY)}\":\"$myHrSelfServiceLanguage\",",
                "  \"${Constants::E_TIME_URL.name.lowercase(Locale.GERMANY)}\":\"$eTimeUrl\",",
                "  \"${Constants::E_TIME_LANGUAGE.name.lowercase(Locale.GERMANY)}\":\"$eTimeLanguage\",",
                "  \"${Constants::MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT.name.lowercase(Locale.GERMANY)}\":\"$maxDailyWorkTillAutoClockOut\",",
                "  \"${Constants::WEEKDAYS_OFF.name.lowercase(Locale.GERMANY)}\":\"$weekdaysOff\",",
                "  \"${Constants::DAYS_OFF.name.lowercase(Locale.GERMANY)}\":\"$daysOff\",",
                "  \"${Constants::CHROME_PROFILE_PATH.name.lowercase(Locale.GERMANY)}\":\"$chromeProfilePath\"",
                "}")

            Files.createDirectories(Paths.get(
                configFilePath.split("/").toList().dropLast(1).joinToString("/")))
            File(configFilePath).createNewFile()
            FileOutputStream(configFilePath).apply { writeJson(defaultConfig) }

            println("Successfully created config file: $configFilePath")
        }

        private fun OutputStream.writeJson(lines: List<String>) {
            val writer = bufferedWriter()
            lines.forEach {
                writer.write(it)
                writer.newLine()
            }
            writer.flush()
        }
    }

    fun initConstants(verbose: Boolean) {
        checkConfig()
        val reader = File(configFilePath).inputStream().bufferedReader()
        val map = Parser.default().parse(reader) as JsonObject
        Constants.setApplicationProperties(verbose, map)
    }

    private fun checkConfig() {
        if (!File(configFilePath).exists()) {
            throw AbortException("No config file found in $configFilePath", listOf("Use 'java -jar app.jar init' with the according parameters"))
        }
    }
}