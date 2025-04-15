package com.iaenjoyer.employeetimetracker.service;

import com.iaenjoyer.employeetimetracker.model.TimeRecord;
import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.service.pdf.PdfGeneratorService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportGeneratorService {
    private final TimeRecordService timeRecordService;
    private final UserService userService;
    private final PdfGeneratorService pdfGeneratorService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional(readOnly = true)
    public Map<String, Object> generateGeneralReport(LocalDateTime start, LocalDateTime end) {
        List<User> allUsers = userService.findAll();
        List<TimeRecord> records = timeRecordService.findByDateRange(start, end);
        
        Map<String, Object> report = new HashMap<>();
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("totalUsers", allUsers.size());
        report.put("records", records);

        double totalHours = calculateTotalHours(records, start, end);
        double averageHours = calculateAverageHoursPerUser(records, allUsers.size(), start, end);
        Map<String, Double> departmentStats = calculateDepartmentStats(records);

        report.put("totalHours", totalHours);
        report.put("averageHours", averageHours);
        report.put("departmentStats", departmentStats);
        
        return report;
    }

    @Transactional(readOnly = true)
    public byte[] generateGeneralReportPdf(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> reportData = generateGeneralReport(start, end);
        List<TimeRecord> records = (List<TimeRecord>) reportData.get("records");
        return pdfGeneratorService.generateReport("General", null, start, end, records);
    }

    @Transactional(readOnly = true)
    public byte[] generateGeneralReportExcel(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> reportData = generateGeneralReport(start, end);
        List<TimeRecord> records = (List<TimeRecord>) reportData.get("records");
        return generateExcelReport("General", null, start, end, records);
    }

    private byte[] generateExcelReport(String type, String department, LocalDateTime start, LocalDateTime end, List<TimeRecord> records) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte de Tiempo");
            
            // Estilos
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle contentStyle = createContentStyle(workbook);
            
            // Headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Fecha de Inicio", "Fecha de Fin", "Horas", "Estado", "Notas"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Datos
            int rowNum = 1;
            for (TimeRecord record : records) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(record.getStartTime().format(DATE_FORMATTER));
                row.createCell(1).setCellValue(record.getEndTime() != null ? record.getEndTime().format(DATE_FORMATTER) : "En curso");
                row.createCell(2).setCellValue(String.format("%.2f", record.getHours()));
                row.createCell(3).setCellValue(record.getStatus().toString());
                row.createCell(4).setCellValue(record.getNotes() != null ? record.getNotes() : "Sin notas");
            }

            // Auto-ajustar columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Convertir a bytes
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte Excel: " + e.getMessage());
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createContentStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        return style;
    }

    private double calculateTotalHours(List<TimeRecord> records, LocalDateTime start, LocalDateTime end) {
        return records.stream()
                .filter(r -> r.getStartTime().isAfter(start) && r.getEndTime().isBefore(end) 
                        && r.getStatus() == TimeRecord.Status.APPROVED)
                .mapToDouble(r -> Duration.between(r.getStartTime(), r.getEndTime()).toHours())
                .sum();
    }

    private double calculateAverageHoursPerUser(List<TimeRecord> records, int totalUsers, LocalDateTime start, LocalDateTime end) {
        if (totalUsers == 0) return 0;
        return calculateTotalHours(records, start, end) / totalUsers;
    }

    private Map<String, Double> calculateDepartmentStats(List<TimeRecord> records) {
        return records.stream()
                .filter(r -> r.getStatus() == TimeRecord.Status.APPROVED)
                .collect(Collectors.groupingBy(
                        r -> r.getUser().getDepartment(),
                        Collectors.summingDouble(r -> Duration.between(r.getStartTime(), r.getEndTime()).toHours())
                ));
    }
}
