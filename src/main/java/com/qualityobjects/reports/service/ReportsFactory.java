package com.qualityobjects.reports.service;

import java.util.HashMap;
import java.util.Map;

import com.qualityobjects.commons.exception.QORuntimeException;
import com.qualityobjects.reports.service.base.NativeQueryReport;
import com.qualityobjects.reports.service.base.Report;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

@Service
public class ReportsFactory  {

    private Map<String, NativeQueryReport<?>> reportBeans;
    
    @Autowired
    private ApplicationContext context;

    @SuppressWarnings("unchecked")
    private <T> T getReportComponent(String reportName, Class<T> klass) {
        if (reportBeans == null) {
            loadReports();            
        }
        T reportBean = (T)reportBeans.get(reportName);
        if (reportBean == null) {
            throw new QORuntimeException("Report bean is not defined: " + reportName);
        }
        return reportBean;
    }

    @SuppressWarnings("unchecked")
	public <T> NativeQueryReport<T> getReportComponent(String reportName) {
        return (NativeQueryReport<T>) this.getReportComponent(reportName, NativeQueryReport.class);
    }

    @SuppressWarnings("unchecked")
	public <T> NativeQueryReportCsv<T> getCsvReportComponent(String reportName) {
        return (NativeQueryReportCsv<T>) this.getReportComponent(reportName, NativeQueryReportCsv.class);
    }

    @SuppressWarnings("unchecked")
	public <T> NativeQueryReportPdf<T> getPdfReportComponent(String reportName) {
        return (NativeQueryReportPdf<T>) this.getReportComponent(reportName, NativeQueryReportPdf.class);
    }

    private synchronized void loadReports() {
        if (this.reportBeans == null) {
            this.reportBeans = new HashMap<>();
            Map<String, Object> beans = context.getBeansWithAnnotation(Report.class);
            beans.values().stream().map(bean -> NativeQueryReport.class.cast(bean)).forEach(bean -> {
                Report reportName = AnnotationUtils.findAnnotation(bean.getClass(), Report.class);
                this.reportBeans.put(reportName.value(), bean); 
            });
            
        }
    }
}
