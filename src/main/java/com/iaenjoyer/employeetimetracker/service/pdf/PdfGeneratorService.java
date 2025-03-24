package com.iaenjoyer.employeetimetracker.service.pdf;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
public class PdfGeneratorService {

    public byte[] generatePdf(Map<String, Object> data) {
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            // Add title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Reporte de Tiempo", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(Chunk.NEWLINE);

            // Add content based on data
            Font contentFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Paragraph content = new Paragraph(entry.getKey() + ": " + entry.getValue(), contentFont);
                document.add(content);
            }

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error al generar PDF: " + e.getMessage());
        }
    }
}
