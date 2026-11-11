package com.example.exam.service;

import com.example.exam.dto.LiveClassAttendanceReportDto;
import com.example.exam.model.LiveClass;
import com.example.exam.model.LiveClassAttendance;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LiveClassAttendanceExcelService {

    private final LiveClassService liveClassService;

    private final LiveClassAttendanceService attendanceService;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public byte[] exportAttendance(Long classId) throws IOException {

        // Get live class details
        LiveClass liveClass = liveClassService.getLiveClassById(classId);

        // Get attendance records
        List<LiveClassAttendance> attendanceList =
                attendanceService.getClassAttendance(classId);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Attendance Report");

            // =====================================
            // 1. TITLE STYLE
            // =====================================

            CellStyle titleStyle = workbook.createCellStyle();

            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleFont.setColor(IndexedColors.WHITE.getIndex());

            titleStyle.setFont(titleFont);
            titleStyle.setFillForegroundColor(
                    IndexedColors.DARK_BLUE.getIndex());
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            // =====================================
            // 2. HEADER STYLE
            // =====================================

            CellStyle headerStyle = workbook.createCellStyle();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(
                    IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // =====================================
            // 3. DATA STYLE
            // =====================================

            CellStyle dataStyle = workbook.createCellStyle();

            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // =====================================
            // 4. REPORT TITLE
            // =====================================

            Row titleRow = sheet.createRow(0);

            Cell titleCell = titleRow.createCell(0);

            titleCell.setCellValue("LIVE CLASS ATTENDANCE REPORT");
            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                    new org.apache.poi.ss.util.CellRangeAddress(
                            0, 0, 0, 7));

            // =====================================
            // 5. CLASS INFORMATION
            // =====================================

            Row classRow = sheet.createRow(2);

            classRow.createCell(0).setCellValue("Class Title:");
            classRow.createCell(1).setCellValue(liveClass.getTitle());

            Row dateRow = sheet.createRow(3);

            dateRow.createCell(0).setCellValue("Generated On:");
            dateRow.createCell(1).setCellValue(
                    LocalDateTime.now().format(DATE_FORMAT));

            Row totalRow = sheet.createRow(4);

            totalRow.createCell(0).setCellValue("Total Attended Students:");
            totalRow.createCell(1).setCellValue(attendanceList.size());

            // =====================================
            // 6. TABLE HEADERS
            // =====================================

            String[] headers = {
                    "Sl. No.",
                    "Student Name",
                    "Username",
                    "First Joined",
                    "Last Joined",
                    "Last Left",
                    "Total Duration",
                    "Status"
            };

            Row headerRow = sheet.createRow(6);

            for (int i = 0; i < headers.length; i++) {

                Cell cell = headerRow.createCell(i);

                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);

            }

            headerRow.setHeightInPoints(25);

            // =====================================
            // 7. ATTENDANCE DATA
            // =====================================

            int rowIndex = 7;
            int serialNumber = 1;

            for (LiveClassAttendance attendance : attendanceList) {

                LiveClassAttendanceReportDto report =
                        LiveClassAttendanceReportDto.from(attendance);

                Row row = sheet.createRow(rowIndex++);

                String firstJoined = report.joinedAt() != null
                        ? report.joinedAt().format(DATE_FORMAT)
                        : "N/A";

                String lastJoined = report.lastJoinedAt() != null
                        ? report.lastJoinedAt().format(DATE_FORMAT)
                        : "N/A";

                String lastLeft = report.leftAt() != null
                        ? report.leftAt().format(DATE_FORMAT)
                        : "N/A";

                row.createCell(0).setCellValue(serialNumber++);
                row.createCell(1).setCellValue(report.studentName());
                row.createCell(2).setCellValue(report.username());
                row.createCell(3).setCellValue(firstJoined);
                row.createCell(4).setCellValue(lastJoined);
                row.createCell(5).setCellValue(lastLeft);
                row.createCell(6).setCellValue(report.formattedDuration());

                row.createCell(7).setCellValue(
                        report.currentlyJoined() ? "Currently Joined" : "Left"
                );

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }

            }

            // =====================================
            // 8. COLUMN WIDTH
            // =====================================

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(
                        i,
                        Math.min(sheet.getColumnWidth(i) + 1000, 15000)
                );
            }

            // Keep header visible while scrolling
            sheet.createFreezePane(0, 7);

            // Enable filtering
            if (!attendanceList.isEmpty()) {
                sheet.setAutoFilter(
                        new org.apache.poi.ss.util.CellRangeAddress(
                                6,
                                6 + attendanceList.size(),
                                0,
                                headers.length - 1
                        )
                );
            }

            workbook.write(outputStream);

            return outputStream.toByteArray();

        }
    }
}
