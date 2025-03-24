package com.iaenjoyer.employeetimetracker.service;

import com.iaenjoyer.employeetimetracker.model.TimeRecord;
import com.iaenjoyer.employeetimetracker.model.User;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfGeneratorService {
    private final TimeRecordService timeRecordService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generatePersonalReport(User user, LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);

            document.open();
            addTitle(document, "Reporte Personal de Horas Trabajadas");
            addUserInfo(document, user);
            addDateRange(document, startDate, endDate);

            List<TimeRecord> records = timeRecordService.findByUserAndStartTimeBetween(user, startDate, endDate);
            addTimeRecordsTable(document, records);
            addSummary(document, records);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF", e);
        }
    }

    public byte[] generateEmployeeReport(LocalDateTime startDate, LocalDateTime endDate) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);

            document.open();
            addTitle(document, "Reporte General de Horas Trabajadas");
            addDateRange(document, startDate, endDate);

            List<TimeRecord> records = timeRecordService.findByDateRange(startDate, endDate);
            addTimeRecordsTable(document, records);
            addSummary(document, records);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el PDF", e);
        }
    }

    public byte[] generatePdf(Map<String, Object> data) throws Exception {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            // Título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Informe de Registro de Tiempo", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Información general
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            document.add(new Paragraph("Departamento: " + data.get("department"), normalFont));
            document.add(new Paragraph("Fecha inicio: " + formatDate((LocalDateTime) data.get("startDate")), normalFont));
            document.add(new Paragraph("Fecha fin: " + formatDate((LocalDateTime) data.get("endDate")), normalFont));
            document.add(Chunk.NEWLINE);

            // Estadísticas
            document.add(new Paragraph("Total de usuarios: " + data.get("totalUsers"), normalFont));
            document.add(new Paragraph("Total de horas: " + data.get("totalHours"), normalFont));
            document.add(new Paragraph("Promedio de horas por usuario: " + data.get("averageHoursPerUser"), normalFont));
            document.add(Chunk.NEWLINE);

            // Tabla de registros
            @SuppressWarnings("unchecked")
            List<TimeRecord> records = (List<TimeRecord>) data.get("records");
            if (records != null && !records.isEmpty()) {
                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setSpacingBefore(20);
                table.setSpacingAfter(20);

                // Encabezados
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
                table.addCell(new PdfPCell(new Phrase("Usuario", headerFont)));
                table.addCell(new PdfPCell(new Phrase("Departamento", headerFont)));
                table.addCell(new PdfPCell(new Phrase("Fecha Inicio", headerFont)));
                table.addCell(new PdfPCell(new Phrase("Horas", headerFont)));
                table.addCell(new PdfPCell(new Phrase("Estado", headerFont)));

                // Datos
                Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
                for (TimeRecord record : records) {
                    table.addCell(new PdfPCell(new Phrase(record.getUser().getName(), dataFont)));
                    table.addCell(new PdfPCell(new Phrase(record.getUser().getDepartment(), dataFont)));
                    table.addCell(new PdfPCell(new Phrase(formatDate(record.getStartTime()), dataFont)));
                    table.addCell(new PdfPCell(new Phrase(String.format("%.2f", record.getHours()), dataFont)));
                    table.addCell(new PdfPCell(new Phrase(record.getStatus().toString(), dataFont)));
                }

                document.add(table);
            }

            document.close();
            return baos.toByteArray();
        }
    }

    public byte[] generateDepartmentReport(String department, LocalDateTime start, LocalDateTime end) throws Exception {
        Map<String, Object> data = Map.of(
            "department", department,
            "startDate", start,
            "endDate", end
        );
        return generatePdf(data);
    }

    private void addTitle(Document document, String title) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Paragraph titleParagraph = new Paragraph(title, titleFont);
        titleParagraph.setAlignment(Element.ALIGN_CENTER);
        titleParagraph.setSpacingAfter(20);
        document.add(titleParagraph);
    }

    private void addUserInfo(Document document, User user) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Paragraph userInfo = new Paragraph();
        userInfo.add(new Chunk("Empleado: " + user.getName() + "\n", font));
        userInfo.add(new Chunk("Departamento: " + user.getDepartment() + "\n", font));
        userInfo.setSpacingAfter(20);
        document.add(userInfo);
    }

    private void addDateRange(Document document, LocalDateTime start, LocalDateTime end) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Paragraph dateRange = new Paragraph();
        dateRange.add(new Chunk("Período: " + start.format(DATE_FORMATTER) + " - " + end.format(DATE_FORMATTER), font));
        dateRange.setSpacingAfter(20);
        document.add(dateRange);
    }

    private void addTimeRecordsTable(Document document, List<TimeRecord> records) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20);
        table.setSpacingAfter(20);

        // Headers
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        table.addCell(new PdfPCell(new Phrase("Empleado", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Fecha", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Horas", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Estado", headerFont)));
        table.addCell(new PdfPCell(new Phrase("Notas", headerFont)));

        // Data
        Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        for (TimeRecord record : records) {
            table.addCell(new PdfPCell(new Phrase(record.getUser().getName(), dataFont)));
            table.addCell(new PdfPCell(new Phrase(record.getStartTime().format(DATE_FORMATTER), dataFont)));
            table.addCell(new PdfPCell(new Phrase(String.format("%.2f", record.getHours()), dataFont)));
            table.addCell(new PdfPCell(new Phrase(record.getStatus().toString(), dataFont)));
            table.addCell(new PdfPCell(new Phrase(record.getNotes() != null ? record.getNotes() : "", dataFont)));
        }

        document.add(table);
    }

    private void addSummary(Document document, List<TimeRecord> records) throws DocumentException {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
        Paragraph summary = new Paragraph();
        
        double totalHours = records.stream()
                .filter(r -> r.getStatus() == TimeRecord.TimeRecordStatus.APPROVED)
                .mapToDouble(TimeRecord::getHours)
                .sum();
        
        summary.add(new Chunk("Total de Horas Aprobadas: " + String.format("%.2f", totalHours), font));
        summary.setSpacingBefore(20);
        document.add(summary);
    }

    private String formatDate(LocalDateTime date) {
        return date.format(DATE_FORMATTER);
    }
}
