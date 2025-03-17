package services

import models.TimeTable
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale

class ExcelService {

    fun parseExcelFile(inputStream: InputStream): List<TimeTable> {
        if (inputStream.available() == 0) {
            throw IllegalArgumentException("The uploaded file is empty")
        }

        val workbook = WorkbookFactory.create(inputStream)
        if (workbook.numberOfSheets == 0) {
            throw IllegalArgumentException("The uploaded Excel file has no sheets")
        }

        val sheet = workbook.getSheetAt(0)
        if (sheet.physicalNumberOfRows == 0) {
            throw IllegalArgumentException("The uploaded Excel file is empty")
        }

        val timeTables = mutableListOf<TimeTable>()
        val processedEntries = HashSet<String>() // Track unique entries to prevent duplicates

        // Skip header row, start from index 1
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue

            if (row.getCell(0) == null || getCellValueAsString(row.getCell(0)).isBlank()) {
                continue
            }

            val timeTable = TimeTable(
                group = getCellValueAsString(row.getCell(0)),
                day = getCellValueAsString(row.getCell(1)),
                date = getCellValueAsString(row.getCell(2)),
                startTime = getCellValueAsString(row.getCell(3)),
                endTime = getCellValueAsString(row.getCell(4)),
                className = getCellValueAsString(row.getCell(5)),
                teacherName = getCellValueAsString(row.getCell(6)),
                room = getCellValueAsString(row.getCell(7))
            )

            // Skip invalid entries
            if (!isValidTimeTable(timeTable)) continue

            // Create a unique key for duplicate detection
            val uniqueKey = "${timeTable.group}|${timeTable.day}|${timeTable.date}|${timeTable.startTime}|${timeTable.className}"

            // Only add if not a duplicate
            if (processedEntries.add(uniqueKey)) {
                timeTables.add(timeTable)
            }
        }

        workbook.close()
        return timeTables
    }

    private fun isValidTimeTable(timeTable: TimeTable): Boolean {
        return timeTable.group.isNotBlank() &&
                timeTable.day.isNotBlank() &&
                timeTable.startTime.isNotBlank() &&
                timeTable.endTime.isNotBlank() &&
                timeTable.className.isNotBlank()
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""

        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue.trim()
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    val date = cell.dateCellValue
                    val formatter = when {
                        isTimeOnly(cell) -> SimpleDateFormat("HH:mm", Locale.getDefault()) // 24-hour time format
                        isDateOnly(cell) -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Date format
                        else -> SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) // Full Date-Time format
                    }
                    formatter.format(date) // Convert Date to String
                } else {
                    // Handle integers vs. decimals appropriately
                    val value = cell.numericCellValue
                    if (value == value.toLong().toDouble()) {
                        value.toLong().toString() // Return as integer if it's a whole number
                    } else {
                        value.toString() // Return as decimal if it has fractional part
                    }
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.BLANK -> ""
            CellType.FORMULA -> {
                try {
                    when (cell.cachedFormulaResultType) {
                        CellType.STRING -> cell.stringCellValue.trim()
                        CellType.NUMERIC -> {
                            if (DateUtil.isCellDateFormatted(cell)) {
                                val date = cell.dateCellValue
                                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(date)
                            } else {
                                cell.numericCellValue.toString()
                            }
                        }
                        CellType.BOOLEAN -> cell.booleanCellValue.toString()
                        else -> ""
                    }
                } catch (e: Exception) {
                    "" // Return empty string on formula error
                }
            }
            else -> ""
        }
    }

    /**
     * Checks if the cell contains only a time value.
     */
    private fun isTimeOnly(cell: Cell): Boolean {
        val formatIndex = cell.cellStyle.dataFormat
        val formatString = cell.sheet.workbook.creationHelper.createDataFormat().getFormat(formatIndex)
        return formatString.contains("h") || formatString.contains("m") || formatString.contains("s") // Time-related patterns
    }

    /**
     * Checks if the cell contains only a date value.
     */
    private fun isDateOnly(cell: Cell): Boolean {
        val formatIndex = cell.cellStyle.dataFormat
        val formatString = cell.sheet.workbook.creationHelper.createDataFormat().getFormat(formatIndex)
        return formatString.contains("y") || formatString.contains("d") || formatString.contains("m") // Date-related patterns
    }
}