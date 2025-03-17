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
        // Skip header row, start from index 1
        for (rowIndex in 1..sheet.lastRowNum) {
            val row = sheet.getRow(rowIndex) ?: continue

            if (row.getCell(0) == null || row.getCell(0).stringCellValue.isBlank()) {
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

            if (timeTable.group.isNotBlank() &&
                timeTable.day.isNotBlank() &&
                timeTable.startTime.isNotBlank() &&
                timeTable.startTime.isNotBlank() &&
                timeTable.className.isNotBlank()
            ) {
                timeTables.add(timeTable)
            }
        }

        workbook.close()
        return timeTables
    }

    private fun getCellValueAsString(cell: Cell?): String {
        if (cell == null) return ""

        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    val date = cell.dateCellValue
                    val formatter = when {
                        isTimeOnly(cell) -> SimpleDateFormat("hh:mm a", Locale.getDefault()) // Time format
                        isDateOnly(cell) -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Date format
                        else -> SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()) // Full Date-Time format
                    }
                    formatter.format(date) // Convert Date to String
                } else {
                    cell.numericCellValue.toString() // Convert Number to String
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.BLANK -> "#"
            else -> ""
        }
    }

    /**
     * Checks if the cell contains only a time value.
     */
    private fun isTimeOnly(cell: Cell): Boolean {
        val formatIndex = cell.cellStyle.dataFormat
        val formatString = cell.sheet.workbook.creationHelper.createDataFormat().getFormat(formatIndex)
        return formatString.contains("h") || formatString.contains("m") // Time-related patterns
    }

    /**
     * Checks if the cell contains only a date value.
     */
    private fun isDateOnly(cell: Cell): Boolean {
        val formatIndex = cell.cellStyle.dataFormat
        val formatString = cell.sheet.workbook.creationHelper.createDataFormat().getFormat(formatIndex)
        return formatString.contains("y") || formatString.contains("d") // Date-related patterns
    }
}