package its.time.tracker.upload

import kotlin.math.max

class BookingPositionResolver {
    companion object {
        private val BOOKING_POSITIONS_MAP = mapOf(
            "ITS meetings" to   listOf("f2f", "allhands", "townhall", "jourfixe", "jourfixe"),
            "DoD" to            listOf("cow"),
            "ProjectA" to       listOf("EPP-007", "EPP-008"),
            "ProjectB" to       listOf("EPP-009", "EPP-123", "EPP-0815", "EPP-17662"),
        )

        fun resolveTopicToBookingPosition(topic: String): String {
            if (topic.startsWith("EDF-")) {
                return topic
            }
            if (topic.startsWith("DVR-")) {
                return "Recruiting"
            }

            BOOKING_POSITIONS_MAP.forEach { (bookingPos, possibleTopics) ->
                if (possibleTopics.any { it.equals(topic, ignoreCase = true) }) {
                    return bookingPos
                }
            }

            println("Found no fitting booking position for work topic '$topic'")
            return "UNKNOWN"
        }

        fun getMaxBookingPosNameLength(): Int {
            return max(12, BOOKING_POSITIONS_MAP.keys.maxOf { it.length })
        }
    }
}