package com.iaenjoyer.employeetimetracker.report.dto;

import java.util.List;

public class FichajeDiaDTO {
    private Integer dia;

    private List<FichajeDetalleDTO> fichajeDetalleDTOs;

    // Getters y Setters
    public Integer getDia() {
        return dia;
    }

    public void setDia(Integer dia) {
        this.dia = dia;
    }

    public List<FichajeDetalleDTO> getFichajeDetalleDTOs() {
        return fichajeDetalleDTOs;
    }

    public void setFichajeDetalleDTOs(List<FichajeDetalleDTO> fichajeDetalleDTOs) {
        this.fichajeDetalleDTOs = fichajeDetalleDTOs;
    }
}