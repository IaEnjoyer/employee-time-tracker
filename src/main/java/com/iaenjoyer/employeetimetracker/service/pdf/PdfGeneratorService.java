package com.iaenjoyer.employeetimetracker.service.pdf;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class PdfGeneratorService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateReportBackUp(LocalDateTime start, LocalDateTime end, List<com.iaenjoyer.employeetimetracker.model.TimeRecord> records) {
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
            document.add(new Paragraph("Período: " + start.format(DATE_FORMATTER) + " - " + end.format(DATE_FORMATTER)));
            document.add(new Paragraph("\n"));

            // Create table
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            
            // Add headers
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            String[] headers = {"Fecha de Inicio", "Fecha de Fin", "Horas"};
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
    public byte[]generateReport(LocalDateTime start, LocalDateTime end, List<com.iaenjoyer.employeetimetracker.model.TimeRecord> records) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            // Fuente
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Font boldFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 10);

            // Título
            Paragraph titulo = new Paragraph("REGISTRO DIARIO DE JORNADA TRABAJADOR", titleFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            document.add(titulo);
            document.add(Chunk.NEWLINE);

            // Datos empresa y trabajador
            PdfPTable datosTable = new PdfPTable(2);
            datosTable.setWidthPercentage(100);

            datosTable.addCell(createCell("Nombre o Razón Social:", boldFont));
            datosTable.addCell(createCell("_________", normalFont));
            datosTable.addCell(createCell("CIF:", boldFont));
            datosTable.addCell(createCell("_________", normalFont));
            datosTable.addCell(createCell("C.C.C.:", boldFont));
            datosTable.addCell(createCell("_________", normalFont));
            datosTable.addCell(createCell("Nombre Trabajador:", boldFont));
            datosTable.addCell(createCell("_________", normalFont));
            datosTable.addCell(createCell("NIF:", boldFont));
            datosTable.addCell(createCell("_________", normalFont));
            datosTable.addCell(createCell("NAF:", boldFont));
            datosTable.addCell(createCell("_________", normalFont));
            datosTable.addCell(createCell("Mes y año:", boldFont));
            datosTable.addCell(createCell("_________", normalFont));

            document.add(datosTable);
            document.add(Chunk.NEWLINE);

            // Tabla de días
            PdfPTable jornadaTable = new PdfPTable(new float[]{1, 2, 2, 2, 2});
            jornadaTable.setWidthPercentage(100);

            jornadaTable.addCell(createHeaderCell("Día"));
            jornadaTable.addCell(createHeaderCell("Hora Entrada Mañana"));
            jornadaTable.addCell(createHeaderCell("Hora Salida Mañana"));
            jornadaTable.addCell(createHeaderCell("Hora Entrada Tarde"));
            jornadaTable.addCell(createHeaderCell("Hora Salida Tarde"));

            for (int dia = 1; dia <= 31; dia++) {
                jornadaTable.addCell(createCell(String.valueOf(dia), normalFont));
                jornadaTable.addCell(createCell(" ", normalFont));
                jornadaTable.addCell(createCell(" ", normalFont));
                jornadaTable.addCell(createCell(" ", normalFont));
                jornadaTable.addCell(createCell(" ", normalFont));
            }

            document.add(jornadaTable);
            document.add(Chunk.NEWLINE);

            // Textos legales
            Paragraph textoLegal1 = new Paragraph("Registro realizado en cumplimiento de la obligación establecida en el Art. 12.4 c) y Art. 34.9 del Real Decreto Legislativo 2/2015 de 23 de Octubre...", normalFont);
            textoLegal1.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(textoLegal1);

            document.add(Chunk.NEWLINE);

            // Totales y firmas
            PdfPTable firmasTable = new PdfPTable(2);
            firmasTable.setWidthPercentage(100);

            firmasTable.addCell(createCell("Total Mes: ___________", normalFont));
            firmasTable.addCell(createCell("Firma de la Empresa: ___________", normalFont));
            firmasTable.addCell(createCell("Firma del Trabajador: ___________", normalFont));
            firmasTable.addCell(createCell(" ", normalFont));

            document.add(firmasTable);

            document.close();
            System.out.println("PDF creado exitosamente.");
            return out.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Error al generar el reporte PDF: " + e.getMessage());
        }
    }

    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.BOX);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createHeaderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD)));
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        return cell;
    }
}
