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
import java.time.YearMonth;
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
        if (hasActiveTimeRecord(user)) {
            // Instead of throwing an exception, end the existing record and start a new one
            TimeRecord existingRecord = findActiveRecord(user).get();
            existingRecord.setEndTime(LocalDateTime.now());
            return timeRecordRepository.save(existingRecord);
        }

        TimeRecord record = new TimeRecord();
        record.setUser(user);
        record.setStartTime(LocalDateTime.now());
        record.setIp(ipAddress);
        record.setHours(0d);
        record.setDispositivo(deviceInfo); // Campo nuevo
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
        return timeRecordRepository.save(record);
    }

    private List<RegistroFichajeDTO> convertirAregistroFichajeDTO(Map<YearMonth, Map<Integer, List<TimeRecord>>> registrosFichajes) {
        List<RegistroFichajeDTO> registros = new ArrayList<>();

        for (Map.Entry<YearMonth, Map<Integer, List<TimeRecord>>> entryYearMonth : registrosFichajes.entrySet()) {
            YearMonth yearMonth = entryYearMonth.getKey();
            Map<Integer, List<TimeRecord>> dayGroup = entryYearMonth.getValue();

            RegistroFichajeDTO registroMes = new RegistroFichajeDTO();
            registroMes.setMes(yearMonth.getMonthValue());
            registroMes.setAnio(yearMonth.getYear());

            double totalEstablecidas = dayGroup.values().stream()
                    .flatMap(List::stream)
                    .mapToDouble(r -> r.getHours())
                    .sum();

            double totalOrdinarias = dayGroup.values().size()*8;

            registroMes.setTotalEstablecidas(totalEstablecidas);
            registroMes.setTotalOrdinarias(totalOrdinarias);

            List<FichajeDiaDTO> dias = new ArrayList<>();

            for (Map.Entry<Integer, List<TimeRecord>> entryDay : dayGroup.entrySet()) {
                int dia = entryDay.getKey();
                List<TimeRecord> recordsOfDay = entryDay.getValue();

                FichajeDiaDTO fichajeDia = new FichajeDiaDTO();
                fichajeDia.setDia(dia);

                List<FichajeDetalleDTO> detalles = new ArrayList<>();

                for (TimeRecord record : recordsOfDay) {
                    // Entrada
                    if (record.getStartTime() != null) {
                        detalles.add(new FichajeDetalleDTO(
                                record.getId(),
                                "entrada",
                                record.getStartTime(),
                                record.getIp(),
                                record.getDispositivo()));
                    }

                    // Salida
                    if (record.getEndTime() != null) {
                        detalles.add(new FichajeDetalleDTO(
                                record.getId(),
                                "salida",
                                record.getEndTime(),
                                record.getIp(),
                                record.getDispositivo()));
                    }
                }

                fichajeDia.setFichajeDetalleDTOs(detalles);
                dias.add(fichajeDia);
            }

            registroMes.setFichajesDia(dias);
            registros.add(registroMes);
        }
        return registros;
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
                        }
                    }
    
                    fichajeDia.setFichajeDetalleDTOs(detalles);
                    dias.add(fichajeDia);
                }
    
                registro.setFichajesDia(dias);
    
                // Calcular totales (puedes ajustar esta lógica según tus reglas)
                double totalEstablecidas = dias.size() * 8.0; // Ejemplo simple
                double totalOrdinarias = dias.size() * 8.0;
    
                registro.setTotalEstablecidas(totalEstablecidas);
                registro.setTotalOrdinarias(totalOrdinarias);
    
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
