package its.time.tracker.upload

import kotlin.math.max

class BookingPositionResolver {
    companion object {
        private val BOOKING_POSITIONS_MAP = mapOf(
            // for testing
            "ITS meetings" to   listOf("f2ff", "allhandss", "townhalll", "jourfixee", "jourfixee"),
            "DoD" to            listOf("coww"),
            "ProjectA" to       listOf("EPP-007", "EPP-008"),
            "ProjectB" to       listOf("EPP-009", "EPP-123", "EPP-0815", "EPP-17662"),

            // real map
            "PO PEw Cookbook" to    listOf("orga", "EPP-17899", "sprintwechsel", "EPP-17901", "retro", "EPP-14989", "refinement", "EPP-19251", "postscan_est", "EPP-19253"),
            "Wartung" to            listOf("cow"),
            "Training" to           listOf("weiterbildung"),
            "Line Activity" to      listOf("onboarding"),
            "Meeting" to            listOf("orga", "sdb-jourfixe", "allhands", "roadshow"),
            "Dev 2022" to           listOf(),
        )

        fun resolveTopicToBookingPosition(topic: String): String {
            if (topic.startsWith("EDF-")) {
                return "Wartung"
            }
            if (topic.startsWith("DVR-")) {
                return "Line Activity"
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