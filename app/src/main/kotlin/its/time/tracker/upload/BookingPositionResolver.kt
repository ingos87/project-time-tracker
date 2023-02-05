package its.time.tracker.upload

import kotlin.math.max

class BookingPositionResolver {
    companion object {
        // TODO move this to config file
        private val BOOKING_POSITIONS_MAP = mapOf(
            // for testing
            "ITS meetings" to   listOf("f2ff", "allhandss", "townhalll", "jourfixee", "jourfixee"),
            "DoD" to            listOf("coww"),
            "ProjectA" to       listOf("EPP-007", "EPP-008"),
            "ProjectB" to       listOf("EPP-009", "EPP-123", "EPP-0815", "EPP-17662"),

            // actual projects
            "Laufzeitrabatt"                            to listOf(),
            "Update InfBrok100"                         to listOf(),
            "Hybrider Rückschein"                       to listOf(),
            "Postbuch Optimierung nach Go-Live 2022"    to listOf(),
            "Po-PE1: Cookbook (1a+1b)"                  to listOf("orga", "sprintwechsel", "retro", "EPP-14989", "refinement", "EPP-19251", "postscan_est", "EPP-19253"),
            "Po-PE2: Prod. der Vorverarbeitungsstufe"   to listOf("pi_planning_feb23"),
            "Po : Rückbau EPP"                          to listOf("EPP-17899", "EPP-17901"),
            "Project Placeholder"                       to listOf(),

            // misc
            "IT-Systemerweiterung - Q1"                 to listOf(),
            "Wartung"                                   to listOf("cow"),

            // company stuff
            "Meeting"                                   to listOf("orga", "sdb-jourfixe", "allhands", "roadshow"),
            "Line Activity"                             to listOf("onboarding", "leistungsbeurteilung", "arbeitsschutz"),
            "Training"                                  to listOf("weiterbildung"),

            // absence
            "Available-not assigned"                    to listOf(),
            "Travel"                                    to listOf(),
            "Holidays"                                  to listOf(),
            "Sickness"                                  to listOf(),
            "Work.Time Compensation"                    to listOf(),
            "Other absence"                             to listOf(),
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

            println("Found no fitting booking position for work topic '$topic' -> using cost assessment 'Project Placeholder'")
            return "Project Placeholder"
        }

        fun getMaxBookingPosNameLength(): Int {
            return max(12, BOOKING_POSITIONS_MAP.keys.maxOf { it.length })
        }
    }
}