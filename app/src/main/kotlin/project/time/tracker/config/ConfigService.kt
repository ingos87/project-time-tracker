package project.time.tracker.config

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import project.time.tracker.config.Constants.Companion.COST_ASSMNT_ABSC_KEY
import project.time.tracker.config.Constants.Companion.COST_ASSMNT_DEV_KEY
import project.time.tracker.config.Constants.Companion.COST_ASSMNT_INT_KEY
import project.time.tracker.config.Constants.Companion.COST_ASSMNT_MAINT_KEY
import project.time.tracker.domain.CostAssessmentSetup
import project.time.tracker.exception.AbortException
import org.json.JSONObject
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
            csvPath: String = "/Users/me/times.csv",
            myHrSelfServiceUrl: String = "https://someurl.de",
            myHrSelfServiceLanguage: String = "EN",
            eTimeUrl: String = "https://someurl.de",
            eTimeLanguage: String = "EN",
            maxDailyWorkTillAutoClockOut: String = "",
            weekdaysOff: String = "",
            daysOff: List<String> = emptyList(),
            sickLeave: List<String> = emptyList(),
            childSickLeave: List<String> = emptyList(),
            vacation: List<String> = emptyList(),
            chromeProfilePath: String = "",
            standardDailyWorkDuration: String,
            costAssessmentSetup: CostAssessmentSetup,
        ) {
            if (File(configFilePath).exists()) {
                println("$configFilePath already exists.")
                return
            }

            val configMap = mapOf(
                Constants::CSV_PATH.name.lowercase(Locale.GERMANY) to csvPath,
                Constants::MY_HR_SELF_SERVICE_URL.name.lowercase(Locale.GERMANY) to myHrSelfServiceUrl,
                Constants::MY_HR_SELF_SERVICE_LANGUAGE.name.lowercase(Locale.GERMANY) to myHrSelfServiceLanguage,
                Constants::E_TIME_URL.name.lowercase(Locale.GERMANY) to eTimeUrl,
                Constants::E_TIME_LANGUAGE.name.lowercase(Locale.GERMANY) to eTimeLanguage,
                Constants::MAX_WORK_DURATION_TILL_AUTO_CLOCKOUT.name.lowercase(Locale.GERMANY) to maxDailyWorkTillAutoClockOut,
                Constants::STANDARD_WORK_DURATION_PER_DAY.name.lowercase(Locale.GERMANY) to standardDailyWorkDuration,
                Constants::WEEKDAYS_OFF.name.lowercase(Locale.GERMANY) to weekdaysOff,
                Constants::DAYS_OFF.name.lowercase(Locale.GERMANY) to daysOff,
                Constants::SICK_LEAVE.name.lowercase(Locale.GERMANY) to sickLeave,
                Constants::CHILD_SICK_LEAVE.name.lowercase(Locale.GERMANY) to childSickLeave,
                Constants::VACATION.name.lowercase(Locale.GERMANY) to vacation,
                Constants::CHROME_PROFILE_PATH.name.lowercase(Locale.GERMANY) to chromeProfilePath,
                Constants::COST_ASSESSMENT_SETUP.name.lowercase(Locale.GERMANY) to mapOf<String, Any>(
                    COST_ASSMNT_DEV_KEY to costAssessmentSetup.developmentProjects.associate { it.title to it.abbreviation },
                    COST_ASSMNT_MAINT_KEY to costAssessmentSetup.maintenanceProjects.associate { it.title to it.abbreviation },
                    COST_ASSMNT_INT_KEY to costAssessmentSetup.internalProjects.associate { it.title to it.abbreviation },
                    COST_ASSMNT_ABSC_KEY to costAssessmentSetup.absenceProjects.associate { it.title to it.abbreviation },
                ),
            )

            Files.createDirectories(Paths.get(
                configFilePath.split("/").toList().dropLast(1).joinToString("/")))
            File(configFilePath).createNewFile()
            FileOutputStream(configFilePath).apply { writeJson(JSONObject(configMap).toString(4)) }

            println("Successfully created config file: $configFilePath")
        }

        private fun OutputStream.writeJson(json: String) {
            val writer = bufferedWriter()
            writer.write(json)
            writer.flush()
        }
    }

    fun initConstants(verbose: Boolean) {
        checkConfig()
        val reader = File(configFilePath).inputStream().bufferedReader()
        val map = Parser.default().parse(reader) as JsonObject
        Constants.setApplicationProperties(map, verbose)
    }

    private fun checkConfig() {
        if (!File(configFilePath).exists()) {
            throw AbortException("No config file found in $configFilePath", listOf("Use 'java -jar app.jar init' with the according parameters"))
        }
    }
}