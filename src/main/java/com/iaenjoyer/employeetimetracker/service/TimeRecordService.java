package com.iaenjoyer.employeetimetracker.service;

import com.iaenjoyer.employeetimetracker.model.TimeRecord;
import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.repository.TimeRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class TimeRecordService {
    private static final int RECENT_RECORDS_LIMIT = 10;
    
    private final TimeRecordRepository timeRecordRepository;
    private final UserService userService;
    private final NotificationService notificationService;
    private static final Logger log = Logger.getLogger(TimeRecordService.class.getName());

    @Transactional(readOnly = true)
    public List<TimeRecord> findByUserAndStartTimeBetween(User user, LocalDateTime start, LocalDateTime end) {
        return timeRecordRepository.findByUserAndStartTimeBetween(user, start, end);
    }

    @Transactional(readOnly = true)
    public Optional<TimeRecord> findActiveRecord(User user) {
        return timeRecordRepository.findByUserAndEndTimeIsNull(user);
    }

    @Transactional(readOnly = true)
    public List<TimeRecord> findByDateRange(LocalDateTime start, LocalDateTime end) {
        return timeRecordRepository.findByStartTimeBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<TimeRecord> findByDepartment(String department) {
        List<User> departmentUsers = userService.findByDepartment(department);
        return timeRecordRepository.findByUserIn(departmentUsers);
    }

    @Transactional(readOnly = true)
    public List<TimeRecord> findRecentRecords() {
        return timeRecordRepository.findAllByOrderByStartTimeDesc(PageRequest.of(0, RECENT_RECORDS_LIMIT));
    }

    @Transactional(readOnly = true)
    public List<TimeRecord> findByDepartmentAndDateRange(String department, LocalDateTime start, LocalDateTime end) {
        return timeRecordRepository.findByUserDepartmentAndStartTimeBetweenOrderByStartTimeDesc(department, start, end);
    }

    @Transactional(readOnly = true)
    public boolean hasActiveTimeRecord(User user) {
        Optional<TimeRecord> activeRecord = findActiveRecord(user);
        return activeRecord.isPresent();
    }

    @Transactional
    public TimeRecord approveTimeRecord(Long id) {
        TimeRecord record = timeRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado"));
        
        record.setStatus(TimeRecord.Status.APPROVED);
        notificationService.sendApprovalNotification(record.getUser());
        return timeRecordRepository.save(record);
    }

    @Transactional
    public TimeRecord rejectTimeRecord(Long id, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Se requiere una razón para el rechazo");
        }

        TimeRecord record = timeRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registro no encontrado"));
        
        record.setStatus(TimeRecord.Status.REJECTED);
        record.setRejectionReason(reason);
        notificationService.sendRejectionNotification(record.getUser(), reason);
        return timeRecordRepository.save(record);
    }

    @Transactional
    public TimeRecord startTimeRecord(User user) {
        // Verify if an active record already exists
        if (hasActiveTimeRecord(user)) {
            // Instead of throwing an exception, end the existing record and start a new one
            TimeRecord existingRecord = findActiveRecord(user).get();
            existingRecord.setEndTime(LocalDateTime.now());
            existingRecord.setStatus(TimeRecord.Status.PENDING);
            timeRecordRepository.save(existingRecord);
        }

        TimeRecord record = new TimeRecord();
        record.setUser(user);
        record.setStartTime(LocalDateTime.now());
        record.setStatus(TimeRecord.Status.PENDING);
        return timeRecordRepository.save(record);
    }

    @Transactional
    public TimeRecord endTimeRecord(User user) {
        Optional<TimeRecord> activeRecord = findActiveRecord(user);
        if (activeRecord.isEmpty()) {
            throw new IllegalStateException("No hay un registro activo para este usuario");
        }

        TimeRecord record = activeRecord.get();
        record.setEndTime(LocalDateTime.now());
        record.setStatus(TimeRecord.Status.PENDING);
        return timeRecordRepository.save(record);
    }

    public byte[] generateReport(User user, LocalDateTime start, LocalDateTime end) {
        List<TimeRecord> records = findByUserAndStartTimeBetween(user, start, end);
        
        try {
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, baos);
            
            document.open();
            
            // Add title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD);
            Paragraph title = new Paragraph("Reporte de Tiempo", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));
            
            // Add user info
            document.add(new Paragraph("Usuario: " + user.getUsername()));
            document.add(new Paragraph("Período: " + start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + 
                                    " - " + end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
            document.add(new Paragraph("\n"));
            
            // Create table
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            
            // Add headers
            Stream.of("Fecha de Inicio", "Fecha de Fin", "Horas", "Estado", "Notas")
                .forEach(columnTitle -> {
                    PdfPCell header = new PdfPCell();
                    header.setBackgroundColor(BaseColor.LIGHT_GRAY);
                    header.setBorderWidth(2);
                    header.setPhrase(new Phrase(columnTitle));
                    table.addCell(header);
                });
            
            // Add records
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (TimeRecord record : records) {
                table.addCell(record.getStartTime().format(formatter));
                table.addCell(record.getEndTime() != null ? record.getEndTime().format(formatter) : "En curso");
                table.addCell(String.format("%.2f", record.getHours()));
                table.addCell(record.getStatus().toString());
                table.addCell(record.getNotes() != null ? record.getNotes() : "Sin notas");
            }
            
            document.add(table);
            document.add(new Paragraph("\n"));
            
            // Add summary
            double totalHours = records.stream().mapToDouble(TimeRecord::getHours).sum();
            document.add(new Paragraph("Total de Registros: " + records.size()));
            document.add(new Paragraph("Total de Horas: " + String.format("%.2f", totalHours)));
            
            document.close();
            
            return baos.toByteArray();
        } catch (Exception e) {
            log.severe("Error generating PDF report: " + e.getMessage());
            throw new RuntimeException("Error generating PDF report", e);
        }
    }
}
