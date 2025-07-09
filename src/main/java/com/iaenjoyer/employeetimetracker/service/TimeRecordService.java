package com.iaenjoyer.employeetimetracker.service;

import java.util.Map;

import com.iaenjoyer.employeetimetracker.model.TimeRecord;
import com.iaenjoyer.employeetimetracker.model.User;
import com.iaenjoyer.employeetimetracker.report.dto.FichajeDetalleDTO;
import com.iaenjoyer.employeetimetracker.report.dto.FichajeDiaDTO;
import com.iaenjoyer.employeetimetracker.report.dto.InformeFichajeDTO;
import com.iaenjoyer.employeetimetracker.report.dto.RegistroFichajeDTO;
import com.iaenjoyer.employeetimetracker.report.service.JasperFichajesService;
import com.iaenjoyer.employeetimetracker.repository.TimeRecordRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimeRecordService {
    private static final int RECENT_RECORDS_LIMIT = 10;

    private final TimeRecordRepository timeRecordRepository;
    private final UserService userService;
    private final JasperFichajesService jasperFichajesService;
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
    public TimeRecord startTimeRecord(User user, String ipAddress, String deviceInfo) {
        // Verify if an active record already exists
        TimeRecord record = new TimeRecord();
        ZonedDateTime nowInMadrid = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));
        if (hasActiveTimeRecord(user)) {
            // Instead of throwing an exception, end the existing record and start a new one
            TimeRecord existingRecord = findActiveRecord(user).get();
            existingRecord.setEndTime(nowInMadrid.toLocalDateTime());
            return timeRecordRepository.save(existingRecord);
        }

        record.setUser(user);
        record.setStartTime(nowInMadrid.toLocalDateTime());
        record.setIp(ipAddress);
        record.setHours(0d);
        record.setDispositivo(deviceInfo); // Campo nuevo
        return timeRecordRepository.save(record);
    }

    @Transactional
    public TimeRecord endTimeRecord(User user) {
        Optional<TimeRecord> activeRecord = findActiveRecord(user);
        TimeRecord record = activeRecord.get();
        ZonedDateTime nowInMadrid = ZonedDateTime.now(ZoneId.of("Europe/Madrid"));
        if (activeRecord.isEmpty()) {
            throw new IllegalStateException("No hay un registro activo para este usuario");
        }
        record.setEndTime(nowInMadrid.toLocalDateTime());
        return timeRecordRepository.save(record);
    }

    public List<InformeFichajeDTO> convertToInformeFichajeDTO(List<TimeRecord> timeRecords) {
        if (timeRecords == null || timeRecords.isEmpty()) {
            return Collections.emptyList();
        }
    
        // Agrupar por usuario
        Map<User, List<TimeRecord>> recordsByUser = timeRecords.stream()
            .collect(Collectors.groupingBy(TimeRecord::getUser));
    
        List<InformeFichajeDTO> informes = new ArrayList<>();
    
        for (Map.Entry<User, List<TimeRecord>> entry : recordsByUser.entrySet()) {
            User user = entry.getKey();
            List<TimeRecord> recordsForUser = entry.getValue();
    
            InformeFichajeDTO informe = new InformeFichajeDTO();
            informe.setNombreTrabajador(user.getName());
            informe.setNif(user.getNif());
    
            // Agrupar por mes/anio
            Map<String, List<TimeRecord>> recordsByMonthYear = recordsForUser.stream()
                .filter(r -> r.getStartTime() != null)
                .collect(Collectors.groupingBy(record ->
                    record.getStartTime().getMonthValue() + "-" + record.getStartTime().getYear()
                ));
    
            List<RegistroFichajeDTO> registros = new ArrayList<>();
    
            for (Map.Entry<String, List<TimeRecord>> monthEntry : recordsByMonthYear.entrySet()) {
                String[] parts = monthEntry.getKey().split("-");
                int mes = Integer.parseInt(parts[0]);
                int anio = Integer.parseInt(parts[1]);
                Duration totalDuration = Duration.ZERO; // Acumulador de tiempo
    
                RegistroFichajeDTO registro = new RegistroFichajeDTO();
                registro.setMes(mes);
                registro.setAnio(anio);
    
                // Agrupar por día
                Map<Integer, List<TimeRecord>> recordsByDay = monthEntry.getValue().stream()
                    .collect(Collectors.groupingBy(record ->
                        record.getStartTime().getDayOfMonth()
                    ));
    
                List<FichajeDiaDTO> dias = new ArrayList<>();
    
                for (Map.Entry<Integer, List<TimeRecord>> dayEntry : recordsByDay.entrySet()) {
                    FichajeDiaDTO fichajeDia = new FichajeDiaDTO();
                    fichajeDia.setDia(dayEntry.getKey());
    
                    List<FichajeDetalleDTO> detalles = new ArrayList<>();
                    LocalDateTime entrada = null;
                    LocalDateTime salida = null;
                    
                    for (TimeRecord record : dayEntry.getValue()) {
                        // Entrada
                        if (record.getStartTime() != null) {
                            detalles.add(new FichajeDetalleDTO(
                                record.getId(),
                                "entrada",
                                record.getStartTime(),
                                record.getIp(),
                                record.getDispositivo()
                            ));
                            entrada = record.getStartTime();
                        }
    
                        // Salida
                        if (record.getEndTime() != null) {
                            detalles.add(new FichajeDetalleDTO(
                                record.getId(),
                                "salida",
                                record.getEndTime(),
                                record.getIp(),
                                record.getDispositivo()
                            ));
                            salida = record.getEndTime();
                        }
                        if (entrada != null && salida != null) {
                            totalDuration = totalDuration.plus(Duration.between(entrada, salida));
                        }
                    }
    
                    fichajeDia.setFichajeDetalleDTOs(detalles);
                    dias.add(fichajeDia);
                }
    
                registro.setFichajesDia(dias);
    
                // Calcular totales (puedes ajustar esta lógica según tus reglas)
                double totalEstablecidas = dias.size() * 8.0; // Ejemplo simple
    
                registro.setTotalEstablecidas(totalEstablecidas);
                registro.setTotalOrdinarias(Math.round(totalDuration.toMinutes()/60d * 100.0) / 100.0);
    
                registros.add(registro);
            }
    
            informe.setListaRegistros(registros);
            informes.add(informe);
        }
    
        return informes;
    }

    public byte[] generateReport(User user, LocalDateTime start, LocalDateTime end) {
        List<TimeRecord> records = findByUserAndStartTimeBetween(user, start, end);

        try {

            List<InformeFichajeDTO> informesFichajeDTO = convertToInformeFichajeDTO(records);
            
            return jasperFichajesService.generarRerporteFichaje(informesFichajeDTO);
        } catch (Exception e) {
            log.severe("Error generating PDF report: " + e.getMessage());
            throw new RuntimeException("Error generating PDF report", e);
        }
    }

    public byte[] generateReport(LocalDateTime start, LocalDateTime end) {
        List<TimeRecord> records = findByDateRange(start, end);

        try {

            List<InformeFichajeDTO> informesFichajeDTO = convertToInformeFichajeDTO(records);
            
            return jasperFichajesService.generarRerporteFichaje(informesFichajeDTO);
        } catch (Exception e) {
            log.severe("Error generating PDF report: " + e.getMessage());
            throw new RuntimeException("Error generating PDF report", e);
        }
    }

    public Duration getTotalWorkedToday(User user) {
        List<TimeRecord> records = timeRecordRepository.findByUserAndStartTimeBetween(
                user,
                LocalDateTime.now().with(LocalTime.MIN),
                LocalDateTime.now());

        Duration total = Duration.ZERO;

        for (TimeRecord record : records) {
            if (record.getStartTime() != null) {
                LocalDateTime end = record.getEndTime() != null ? record.getEndTime() : LocalDateTime.now();
                total = total.plus(Duration.between(record.getStartTime(), end));
            }
        }

        return total;
    }
}
