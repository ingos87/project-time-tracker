package its.time.tracker.service

import its.time.tracker.service.util.ClockEvent
import its.time.tracker.service.util.DateTimeUtil
import its.time.tracker.service.util.EventType
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDateTime

const val TMP_CSV_PATH = "/tmp/its_times_tmp.csv"

class CsvService(
    private val verbose: Boolean,
    private val csvPath: String
) {

    fun loadClockEvents(fileName: String = csvPath): MutableList<ClockEvent> {
        if (!File(fileName).exists()) {
            if (verbose) println("nothing found at $fileName. Will create new csv file in the process")
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

        if (verbose) println("loaded ${clockEvents.size} clock events from $fileName")
        return clockEvents
    }

    fun saveClockEvents(clockEvents: MutableList<ClockEvent>) {
        // always save sorted
        clockEvents.sortBy { it.dateTime }

        File(TMP_CSV_PATH).createNewFile()
        FileOutputStream(TMP_CSV_PATH).apply { writeCsv(clockEvents) }

        File(csvPath).delete()
        File(TMP_CSV_PATH).renameTo(File(csvPath))

        if (verbose) println("wrote ${clockEvents.size} events to $csvPath")
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