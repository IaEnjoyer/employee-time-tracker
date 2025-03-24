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

    @Transactional(readOnly = true)
    public Map<String, Object> generateDepartmentReport(String department, LocalDateTime start, LocalDateTime end) {
        List<User> departmentUsers = userService.getUsersByDepartment(department);
        List<TimeRecord> records = timeRecordService.findByDepartmentAndDateRange(department, start, end);
        
        Map<String, Object> report = new HashMap<>();
        report.put("department", department);
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("totalUsers", departmentUsers.size());
        report.put("records", records);

        // Calculate statistics
        double totalHours = records.stream()
                .filter(r -> r.getStartTime().isAfter(start) && r.getEndTime().isBefore(end) 
                        && r.getStatus() == TimeRecord.Status.APPROVED)
                .mapToDouble(r -> Duration.between(r.getStartTime(), r.getEndTime()).toHours())
                .sum();
        
        long approvedRecords = records.stream()
                .filter(r -> r.getStartTime().isAfter(start) && r.getEndTime().isBefore(end) 
                        && r.getStatus() == TimeRecord.Status.APPROVED)
                .count();
        
        long rejectedRecords = records.stream()
                .filter(r -> r.getStartTime().isAfter(start) && r.getEndTime().isBefore(end) 
                        && r.getStatus() == TimeRecord.Status.REJECTED)
                .count();

        report.put("totalHours", totalHours);
        report.put("approvedRecords", approvedRecords);
        report.put("rejectedRecords", rejectedRecords);
        report.put("totalRecords", records.size());

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateGeneralReport(LocalDateTime start, LocalDateTime end) {
        List<User> allUsers = userService.findAll();
        List<TimeRecord> records = timeRecordService.findByDateRange(start, end);
        
        Map<String, Object> report = new HashMap<>();
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("totalUsers", allUsers.size());
        report.put("records", records);

        // Calculate statistics by department
        Map<String, List<TimeRecord>> recordsByDepartment = records.stream()
                .collect(Collectors.groupingBy(r -> r.getUser().getDepartment()));

        Map<String, Map<String, Object>> departmentStats = new HashMap<>();
        for (Map.Entry<String, List<TimeRecord>> entry : recordsByDepartment.entrySet()) {
            String dept = entry.getKey();
            List<TimeRecord> deptRecords = entry.getValue();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalHours", deptRecords.stream()
                    .filter(r -> r.getStartTime().isAfter(start) && r.getEndTime().isBefore(end) 
                            && r.getStatus() == TimeRecord.Status.APPROVED)
                    .mapToDouble(r -> Duration.between(r.getStartTime(), r.getEndTime()).toHours())
                    .sum());
            stats.put("approvedRecords", deptRecords.stream()
                    .filter(r -> r.getStartTime().isAfter(start) && r.getEndTime().isBefore(end) 
                            && r.getStatus() == TimeRecord.Status.APPROVED)
                    .count());
            stats.put("rejectedRecords", deptRecords.stream()
                    .filter(r -> r.getStartTime().isAfter(start) && r.getEndTime().isBefore(end) 
                            && r.getStatus() == TimeRecord.Status.REJECTED)
                    .count());
            stats.put("totalRecords", deptRecords.size());

            departmentStats.put(dept, stats);
        }

        report.put("departmentStats", departmentStats);
        return report;
    }

    @Transactional(readOnly = true)
    public byte[] generateOfficialReport(LocalDateTime start, LocalDateTime end) {
        return generateExcelReport(null, start, end);
    }

    @Transactional(readOnly = true)
    public byte[] generateOfficialDepartmentReport(String department, LocalDateTime start, LocalDateTime end) {
        return generateExcelReport(department, start, end);
    }

    public byte[] generateDepartmentReportPdf(String department, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> data = generateDepartmentReport(department, start, end);
        return pdfGeneratorService.generatePdf(data);
    }

    public byte[] generateGeneralReportPdf(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> data = generateGeneralReport(start, end);
        return pdfGeneratorService.generatePdf(data);
    }

    public byte[] generateDepartmentReportExcel(String department, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> data = generateDepartmentReport(department, start, end);
        return generateExcelReport(data);
    }

    public byte[] generateGeneralReportExcel(LocalDateTime start, LocalDateTime end) {
        Map<String, Object> data = generateGeneralReport(start, end);
        return generateExcelReport(data);
    }

    private byte[] generateExcelReport(Map<String, Object> data) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte de Tiempo");
            
            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Usuario", "Departamento", "Fecha", "Horas", "Estado"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Datos
            List<TimeRecord> records = (List<TimeRecord>) data.get("records");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowNum = 1;
            for (TimeRecord record : records) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(record.getUser().getName());
                row.createCell(1).setCellValue(record.getUser().getDepartment());
                row.createCell(2).setCellValue(record.getStartTime().format(formatter));
                row.createCell(3).setCellValue(Duration.between(record.getStartTime(), record.getEndTime()).toHours());
                row.createCell(4).setCellValue(record.getStatus().toString());
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

    private byte[] generateExcelReport(String department, LocalDateTime start, LocalDateTime end) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte de Tiempo");
            
            // Estilos
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            // Headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Usuario", "Departamento", "Fecha", "Horas", "Estado"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }

            // Datos
            List<TimeRecord> records;
            if (department != null) {
                records = timeRecordService.findByDepartment(department);
            } else {
                records = timeRecordService.findByDateRange(start, end);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowNum = 1;
            for (TimeRecord record : records) {
                if ((department != null && record.getUser().getDepartment().equals(department)) 
                        && record.getStartTime().isAfter(start) && record.getEndTime().isBefore(end)) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(record.getUser().getName());
                    row.createCell(1).setCellValue(record.getUser().getDepartment());
                    row.createCell(2).setCellValue(record.getStartTime().format(formatter));
                    row.createCell(3).setCellValue(Duration.between(record.getStartTime(), record.getEndTime()).toHours());
                    row.createCell(4).setCellValue(record.getStatus().toString());
                }
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
