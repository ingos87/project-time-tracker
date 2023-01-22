package its.time.tracker.service

import its.time.tracker.config.Constants
import its.time.tracker.domain.ClockEvent
import its.time.tracker.domain.EventType
import its.time.tracker.util.DateTimeUtil
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime

const val TMP_CSV_PATH = "/tmp/its_times_tmp.csv"

class CsvService {

    fun loadClockEvents(fileName: String = Constants.CSV_PATH): MutableList<ClockEvent> {
        if (!File(fileName).exists()) {
            if (Constants.VERBOSE) println("nothing found at $fileName. Will create new csv file in the process")
            return ArrayList()
        }

        val reader = File(fileName).inputStream().bufferedReader()
        reader.readLine()
        val clockEvents = reader.lineSequence()
            .filter { it.isNotBlank() }
            .map {
                val (dateTime, eventType, topic) = it.split(';', ignoreCase = false, limit = 3)
                ClockEvent(DateTimeUtil.toValidDateTime(dateTime.trim()) as LocalDateTime, EventType.valueOf(eventType.trim()), topic.trim())
            }.toMutableList()

        if (Constants.VERBOSE) println("loaded ${clockEvents.size} clock events from $fileName")
        return clockEvents
    }

    fun saveClockEvents(clockEvents: MutableList<ClockEvent>) {
        // always save sorted
        clockEvents.sortBy { it.dateTime }

        File(TMP_CSV_PATH).createNewFile()
        FileOutputStream(TMP_CSV_PATH).apply { writeCsv(clockEvents) }

        File(Constants.CSV_PATH).delete()
        File(TMP_CSV_PATH).renameTo(File(Constants.CSV_PATH))

        if (Constants.VERBOSE) println("wrote ${clockEvents.size} events to ${Constants.CSV_PATH}")
    }

    private fun OutputStream.writeCsv(clockEvents: List<ClockEvent>) {
        val writer = bufferedWriter()
        writer.write(ClockEvent.getCsvHeaderLine())
        writer.newLine()
        clockEvents.forEach {
            writer.write(it.toCsvLine())
            writer.newLine()
        }
        writer.flush()
    }
}