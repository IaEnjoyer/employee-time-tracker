package com.iaenjoyer.employeetimetracker.service.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfGeneratorService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateReport(String type, String department, LocalDateTime start, LocalDateTime end, List<com.iaenjoyer.employeetimetracker.model.TimeRecord> records) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Reporte de Tiempo", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // Add report info
            document.add(new Paragraph("Tipo de Reporte: " + type));
            document.add(new Paragraph("Departamento: " + (department != null ? department : "Todos")));
            document.add(new Paragraph("Período: " + start.format(DATE_FORMATTER) + " - " + end.format(DATE_FORMATTER)));
            document.add(new Paragraph("\n"));

            // Create table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            // Add headers
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            String[] headers = {"Fecha de Inicio", "Fecha de Fin", "Horas", "Estado", "Notas"};
            for (String columnTitle : headers) {
                PdfPCell header = new PdfPCell(new Phrase(columnTitle, headerFont));
                header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                header.setBorderWidth(2);
                table.addCell(header);
            }

            // Add records
            Font contentFont = new Font(Font.FontFamily.HELVETICA, 12);
            for (com.iaenjoyer.employeetimetracker.model.TimeRecord tr : records) {
                table.addCell(new Phrase(tr.getStartTime().format(DATE_FORMATTER), contentFont));
                table.addCell(new Phrase(tr.getEndTime() != null ? tr.getEndTime().format(DATE_FORMATTER) : "En curso", contentFont));
                table.addCell(new Phrase(String.format("%.2f", tr.getHours()), contentFont));
                table.addCell(new Phrase(tr.getStatus().toString(), contentFont));
                table.addCell(new Phrase(tr.getNotes() != null ? tr.getNotes() : "Sin notas", contentFont));
            }

            document.add(table);
            document.add(new Paragraph("\n"));

            // Add summary
            double totalHours = records.stream()
                .mapToDouble(r -> r.getHours())
                .sum();
            document.add(new Paragraph("Total de Registros: " + records.size()));
            document.add(new Paragraph("Total de Horas: " + String.format("%.2f", totalHours)));

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte PDF: " + e.getMessage());
        }
    }
}
