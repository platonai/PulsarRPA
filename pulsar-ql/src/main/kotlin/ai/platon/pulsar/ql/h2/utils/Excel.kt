package ai.platon.pulsar.ql.h2.utils

import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.CreationHelper
import org.apache.poi.xssf.usermodel.XSSFSheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileOutputStream
import java.io.IOException
import java.sql.ResultSet
import java.sql.SQLException

class Excel(
    val name: String,
    val path: String
) {
    fun export(rs: ResultSet) {
        try {
            val workbook = XSSFWorkbook()

            val sheet = workbook.createSheet(name)
            writeHeaderLine(sheet)

            writeDataLines(rs, workbook, sheet)
            val outputStream = FileOutputStream(path)
            workbook.write(outputStream)
            workbook.close()
        } catch (e: IOException) {
            println("File IO error:")
            e.printStackTrace()
        }
    }

    private fun writeHeaderLine(sheet: XSSFSheet) {
        val headerRow = sheet.createRow(0)
        var headerCell = headerRow.createCell(0)
        headerCell.setCellValue("Course Name")
        headerCell = headerRow.createCell(1)
        headerCell.setCellValue("Student Name")
        headerCell = headerRow.createCell(2)
        headerCell.setCellValue("Timestamp")
        headerCell = headerRow.createCell(3)
        headerCell.setCellValue("Rating")
        headerCell = headerRow.createCell(4)
        headerCell.setCellValue("Comment")
    }

    @Throws(SQLException::class)
    private fun writeDataLines(
        result: ResultSet, workbook: XSSFWorkbook,
        sheet: XSSFSheet,
    ) {
        var rowCount = 1
        while (result.next()) {
            val courseName = result.getString("course_name")
            val studentName = result.getString("student_name")
            val rating = result.getFloat("rating")
            val timestamp = result.getTimestamp("timestamp")
            val comment = result.getString("comment")
            val row = sheet.createRow(rowCount++)

            var columnCount = 0
            var cell: Cell = row.createCell(columnCount++)
            cell.setCellValue(courseName)
            cell = row.createCell(columnCount++)
            cell.setCellValue(studentName)
            cell = row.createCell(columnCount++)
            val cellStyle: CellStyle = workbook.createCellStyle()
            val creationHelper: CreationHelper = workbook.creationHelper
            cellStyle.dataFormat = creationHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss")
            cell.setCellStyle(cellStyle)
            cell.setCellValue(timestamp)
            cell = row.createCell(columnCount++)
            cell.setCellValue(rating.toDouble())
            cell = row.createCell(columnCount)
            cell.setCellValue(comment)
        }
    }
}
