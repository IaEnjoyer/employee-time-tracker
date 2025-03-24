package com.iaenjoyer.employeetimetracker.service;

import com.iaenjoyer.employeetimetracker.model.TimeRecord;
import com.iaenjoyer.employeetimetracker.model.User;
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

    @Transactional(readOnly = true)
    public Map<String, Object> generateDepartmentReport(String department, LocalDateTime start, LocalDateTime end) {
        List<User> departmentUsers = userService.getUsersByDepartment(department);
        List<TimeRecord> records = timeRecordService.findByDepartment(department);
        
        Map<String, Object> report = new HashMap<>();
        report.put("department", department);
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("totalUsers", departmentUsers.size());
        report.put("totalHours", calculateTotalHours(records, start, end));
        report.put("averageHoursPerUser", calculateAverageHoursPerUser(records, departmentUsers.size(), start, end));
        report.put("records", records);

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> generateGeneralReport(LocalDateTime start, LocalDateTime end) {
        List<TimeRecord> records = timeRecordService.findByDateRange(start, end);
        List<User> allUsers = userService.findAll();

        Map<String, Object> report = new HashMap<>();
        report.put("startDate", start);
        report.put("endDate", end);
        report.put("totalUsers", allUsers.size());
        report.put("totalHours", calculateTotalHours(records, start, end));
        report.put("averageHoursPerUser", calculateAverageHoursPerUser(records, allUsers.size(), start, end));
        report.put("departmentStats", calculateDepartmentStats(records));
        report.put("records", records);

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
                        && r.getStatus() == TimeRecord.TimeRecordStatus.APPROVED)
                .mapToDouble(r -> Duration.between(r.getStartTime(), r.getEndTime()).toHours())
                .sum();
    }

    private double calculateAverageHoursPerUser(List<TimeRecord> records, int totalUsers, LocalDateTime start, LocalDateTime end) {
        if (totalUsers == 0) return 0;
        return calculateTotalHours(records, start, end) / totalUsers;
    }

    private Map<String, Double> calculateDepartmentStats(List<TimeRecord> records) {
        return records.stream()
                .filter(r -> r.getStatus() == TimeRecord.TimeRecordStatus.APPROVED)
                .collect(Collectors.groupingBy(
                        r -> r.getUser().getDepartment(),
                        Collectors.summingDouble(r -> Duration.between(r.getStartTime(), r.getEndTime()).toHours())
                ));
    }
}
