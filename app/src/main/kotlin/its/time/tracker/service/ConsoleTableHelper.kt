package its.time.tracker.service

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.SortedSet

class ConsoleTableHelper {
    companion object {
        private const val CELL_WIDTH = 6

        fun getHorizontalSeparator(
            uniqueDays: SortedSet<LocalDate>,
            separatorPosition: SeparatorPosition,
            firstColWidth: Int,
            isDoubleLine: Boolean
        ): String {

            val lineElem = if(isDoubleLine) "═" else "─"
            val frontJunction = when(separatorPosition) {
                SeparatorPosition.TOP ->    "┌"
                SeparatorPosition.MIDDLE -> "├"
                SeparatorPosition.BOTTOM -> "└"
            }

            val centerJunction = when(separatorPosition) {
                SeparatorPosition.TOP ->    "┬"
                SeparatorPosition.MIDDLE -> "┼"
                SeparatorPosition.BOTTOM -> "┴"
            }

            val doubleCenterJunction = when(separatorPosition) {
                SeparatorPosition.TOP ->    "╦"
                SeparatorPosition.MIDDLE -> "╬"
                SeparatorPosition.BOTTOM -> "╩"
            }

            val endJunction = when(separatorPosition) {
                SeparatorPosition.TOP ->    "┐"
                SeparatorPosition.MIDDLE -> "┤"
                SeparatorPosition.BOTTOM -> "┘"
            }

            val separatorLine = StringBuilder()
            separatorLine.append(frontJunction + lineElem.repeat(firstColWidth))

            val horizontalLineElem = lineElem.repeat(CELL_WIDTH)
            for (i in uniqueDays.indices) {
                if (needsDoubleLineDueToDaysDiff(uniqueDays, i)) {
                    separatorLine.append(doubleCenterJunction)
                }
                else {
                    separatorLine.append(centerJunction)
                }
                separatorLine.append(horizontalLineElem)
            }

            separatorLine.append(endJunction)

            return separatorLine.toString()
        }

        fun getContentLine(title: String, values: List<String>, uniqueDays: SortedSet<LocalDate>): String {
            val lineBuilder = StringBuilder()
            lineBuilder.append("│$title")

            for (i in values.indices) {
                if (needsDoubleLineDueToDaysDiff(uniqueDays, i)) {
                    lineBuilder.append("║")
                }
                else {
                    lineBuilder.append("│")
                }
                lineBuilder.append(getCellString(values[i], CELL_WIDTH, TextOrientation.CENTER))
            }

            lineBuilder.append("│")

            return lineBuilder.toString()
        }

        fun needsDoubleLineDueToDaysDiff(uniqueDays: SortedSet<LocalDate>, currentIdx: Int): Boolean {
            if (currentIdx > 0) {
                val prevDate = uniqueDays.elementAt(currentIdx-1)
                val date = uniqueDays.elementAt(currentIdx)
                return ChronoUnit.DAYS.between(prevDate, date) > 1
            }
            return false
        }

        fun getCellString(content: String, cellWidth: Int, textOrientation: TextOrientation): String {
            return when (textOrientation) {
                TextOrientation.CENTER -> when (content.length) {
                    0 -> " ".repeat(cellWidth)
                    1 -> " ".repeat(cellWidth-2) + content + " "
                    2 -> " ".repeat(cellWidth-3) + content + " "
                    3 -> " ".repeat(cellWidth-4) + content + " "
                    4 -> " ".repeat(cellWidth-5) + content + " "
                    5 -> " ".repeat(cellWidth-5) + content
                    6 -> " ".repeat(cellWidth-6) + content
                    else -> content.substring(0, cellWidth)
                }
                TextOrientation.LEFT -> {
                    " " + content.padEnd(cellWidth-1)
                }
                TextOrientation.RIGHT -> {
                    " " + content.padStart(cellWidth-1)
                }
            }
        }
    }
}

enum class TextOrientation {
    LEFT, RIGHT, CENTER
}

enum class SeparatorPosition {
    TOP, MIDDLE, BOTTOM
}