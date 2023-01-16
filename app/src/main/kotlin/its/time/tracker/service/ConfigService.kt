package its.time.tracker.service

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ConfigService private constructor(private var configFilePath: String) {

    companion object {
        const val KEY_CSV_PATH = "csv_path"
        const val KEY_MY_HR_SELF_SERVICE_URL = "my_hr_self_service_url"
        const val KEY_E_TIME_URL = "e_time_url"
        const val KEY_DAYS_OFF = "days_off"

        fun createConfigService(configFilePath: String?): ConfigService {
            return ConfigService(configFilePath ?: "./app.json")
        }
    }

    fun getConfigParameterValue(key: String): String {
        val map: MutableMap<String, Any?> = loadConfig()
        val value = map[key]

        if (value != null) {
            return value.toString()
        }
        else {
            throw AbortException("unknown parameter $key")
        }
    }

    private fun loadConfig(): MutableMap<String, Any?> {
        checkConfig()
        val reader = File(configFilePath).inputStream().bufferedReader()
        return Parser.default().parse(reader) as JsonObject
    }

    fun createEmptyConfig(
        csvPath: String = "/Users/me/its_times.csv",
        myHrSelfServiceUrl: String = "https://someurl.de",
        eTimeUrl: String = "https://someurl.de",
        weekdaysOff: String = "SAT,SUN",
    ) {
        if (File(configFilePath).exists()) {
            println("$configFilePath already exists.")
            return
        }

        val defaultConfig = listOf(
            "{",
            "  \"$KEY_CSV_PATH\":\"$csvPath\",",
            "  \"$KEY_MY_HR_SELF_SERVICE_URL\":\"$myHrSelfServiceUrl\",",
            "  \"$KEY_E_TIME_URL\":\"$eTimeUrl\",",
            "  \"$KEY_DAYS_OFF\":\"$weekdaysOff\"",
            "}")

        File(configFilePath).createNewFile()
        FileOutputStream(configFilePath).apply { writeJson(defaultConfig) }

        println("Successfully created config file: $configFilePath")
    }

    private fun OutputStream.writeJson(clockEvents: List<String>) {
        val writer = bufferedWriter()
        clockEvents.forEach {
            writer.write(it)
            writer.newLine()
        }
        writer.flush()
    }

    private fun checkConfig() {
        if (!File(configFilePath).exists()) {
            throw AbortException("No config file found in $configFilePath", listOf("Use 'java -jar app.jar init' with the according parameters"))
        }
    }
}