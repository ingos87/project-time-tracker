package its.time.tracker

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

const val TMP_CSV_PATH = "/tmp/its_times_tmp.csv"
class StartTimeService(
    private val verbose: Boolean,
    private val csvPath: String,
) {

    fun addClockOut(dateTime: String) {
        addClockEvent(ClockEvent(dateTime, EventType.CLOCK_OUT, "", ""))
    }

    fun addClockIn(topic: String, dateTime: String) {
        addClockEvent(ClockEvent(dateTime, EventType.CLOCK_IN, topic, ""))
    }

    private fun addClockEvent(clockEvent: ClockEvent) {
        val clockEvents = loadClockEvents()

        if (clockEvents.any { it.dateTime == clockEvent.dateTime }) {
            // TODO replace event (with same eventType only!) to list if datetime is present
        }

        clockEvents.add(clockEvent)

        saveClockEvents(clockEvents)
    }

    private fun loadClockEvents(fileName: String = csvPath): MutableList<ClockEvent> {
        if (!File(fileName).exists()) {
            if (verbose) println("nothing found at $fileName. Will create new csv file in the process")
            return ArrayList()
        }

        val reader = File(fileName).inputStream().bufferedReader()
        reader.readLine()
        val clockEvents = reader.lineSequence()
            .filter { it.isNotBlank() }
            .map {
                val (dateTime, eventType, topic, bookingPosition) = it.split(';', ignoreCase = false, limit = 4)
                ClockEvent(dateTime.trim(), EventType.valueOf(eventType.trim()), topic.trim(), bookingPosition.trim())
            }.toMutableList()

        if (verbose) println("loaded ${clockEvents.size} clock events from $fileName")
        return clockEvents
    }

    private fun saveClockEvents(clockEvents: MutableList<ClockEvent>) {
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
        writer.write(""""dateTime";"eventType";"topic";"bookingPosition"""")
        writer.newLine()
        clockEvents.forEach {
            writer.write("${it.dateTime};${it.eventType.name};${it.topic};${it.bookingPosition}")
            writer.newLine()
        }
        writer.flush()
    }
}