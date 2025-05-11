package com.iaenjoyer.employeetimetracker.report.dto;

import java.util.List;

public class InformeFichajeDTO {
    String nombreTrabajador;
    String nif;
    List<RegistroFichajeDTO> listaRegistros;
    public InformeFichajeDTO() {
    }
    public InformeFichajeDTO(String nombreTrabajador, String nif, List<RegistroFichajeDTO> listaRegistros) {
        this.nombreTrabajador = nombreTrabajador;
        this.nif = nif;
        this.listaRegistros = listaRegistros;
    }
    public String getNombreTrabajador() {
        return nombreTrabajador;
    }
    public void setNombreTrabajador(String nombreTrabajador) {
        this.nombreTrabajador = nombreTrabajador;
    }
    public String getNif() {
        return nif;
    }
    public void setNif(String nif) {
        this.nif = nif;
    }
    public List<RegistroFichajeDTO> getListaRegistros() {
        return listaRegistros;
    }
    public void setListaRegistros(List<RegistroFichajeDTO> listaRegistros) {
        this.listaRegistros = listaRegistros;
    }
}
