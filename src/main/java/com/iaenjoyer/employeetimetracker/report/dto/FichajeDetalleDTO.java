package com.iaenjoyer.employeetimetracker.report.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FichajeDetalleDTO {
    private Long id;
    private String evento; // "entrada" o "salida"
    private LocalDateTime hora;
    private String horaFormateada;
    private String ip;
    private String dispositivo;

    public FichajeDetalleDTO(Long id, String evento, LocalDateTime hora, String ip, String dispositivo) {
        this.id = id;
        this.evento = evento;
        this.hora = hora;
        this.ip = ip;
        this.dispositivo = dispositivo;
    }
    

    public void setHoraFormateada(String horaFormateada) {
        this.horaFormateada = horaFormateada;
    }
    public String getHoraFormateada() {
        return hora != null ? hora.format(DateTimeFormatter.ofPattern("HH:mm:ss")) : "";
    }

    public FichajeDetalleDTO() {
    }

    // Getters y Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEvento() {
        return evento;
    }

    public void setEvento(String evento) {
        this.evento = evento;
    }

    public LocalDateTime getHora() {
        return hora;
    }

    public void setHora(LocalDateTime hora) {
        this.hora = hora;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }
}
