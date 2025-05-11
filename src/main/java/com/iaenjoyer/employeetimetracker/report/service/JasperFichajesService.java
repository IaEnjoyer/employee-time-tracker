package com.iaenjoyer.employeetimetracker.report.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.iaenjoyer.employeetimetracker.report.dto.InformeFichajeDTO;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Service
public class JasperFichajesService {
    public byte[] generarRerporteFichaje(List<InformeFichajeDTO> iFichajeDTO) throws JRException, IOException{
        ClassPathResource resource = new ClassPathResource("reports/temporal.jrxml");
        JasperReport jasperReport = JasperCompileManager.compileReport(resource.getInputStream());
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(iFichajeDTO);

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,new HashMap<>(),dataSource);
        
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }
}
