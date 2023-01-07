package its.time.tracker.service

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

private const val CONFIG_FILE_PATH = "./app.json"

class ConfigService() {

    companion object {
        const val KEY_CSV_PATH = "csv_path"
        const val KEY_MY_HR_SELF_SERVICE_URL = "my_hr_self_service_url"
        const val E_TIME_URL = "e_time_url"
    }

    fun loadConfig(): MutableMap<String, Any?> {
        if (!File(CONFIG_FILE_PATH).exists()) {
            println("No config file present in same dir as jar. You must create one.")
            println("Use 'java -jar app.jar init'")
            return mutableMapOf()
        }

        val reader = File(CONFIG_FILE_PATH).inputStream().bufferedReader()
        return Parser.default().parse(reader) as JsonObject
    }

    fun createEmptyConfig(
        csvPath: String = "/Users/me/its_times.csv",
        myHrSelfServiceUrl: String = "https://someurl.de",
        eTimeUrl: String = "https://someurl.de",
    ) {
        if (File(CONFIG_FILE_PATH).exists()) {
            println("$CONFIG_FILE_PATH already exists.")
            return
        }

        val defaultConfig = listOf(
            "{",
            "  \"$KEY_CSV_PATH\": \"$csvPath\",",
            "  \"$KEY_MY_HR_SELF_SERVICE_URL\": \"$myHrSelfServiceUrl\"",
            "  \"$E_TIME_URL\": \"$eTimeUrl\"",
            "}")

        File(CONFIG_FILE_PATH).createNewFile()
        FileOutputStream(CONFIG_FILE_PATH).apply { writeJson(defaultConfig) }
    }

    private fun OutputStream.writeJson(clockEvents: List<String>) {
        val writer = bufferedWriter()
        clockEvents.forEach {
            writer.write(it)
            writer.newLine()
        }
        writer.flush()
    }
}