package com.iaenjoyer.employeetimetracker.report.dto;

import java.util.List;

public class RegistroFichajeDTO {
    private Integer mes;
    private Integer anio;

    private List<FichajeDiaDTO> fichajesDia;
    private Double totalEstablecidas;
    private Double totalOrdinarias;

    // Getters y Setters
    public Integer getMes() {
        return mes;
    }

    public void setMes(Integer mes) {
        this.mes = mes;
    }

    public Integer getAnio() {
        return anio;
    }

    public void setAnio(Integer anio) {
        this.anio = anio;
    }

    public List<FichajeDiaDTO> getFichajesDia() {
        return fichajesDia;
    }

    public void setFichajesDia(List<FichajeDiaDTO> fichajesDia) {
        this.fichajesDia = fichajesDia;
    }

    public Double getTotalEstablecidas() {
        return totalEstablecidas;
    }

    public void setTotalEstablecidas(Double totalEstablecidas) {
        this.totalEstablecidas = totalEstablecidas;
    }

    public Double getTotalOrdinarias() {
        return totalOrdinarias;
    }

    public void setTotalOrdinarias(Double totalOrdinarias) {
        this.totalOrdinarias = totalOrdinarias;
    }
}
