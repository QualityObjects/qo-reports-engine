package com.qualityobjects.reports.service;
import liquibase.pro.packaged.T;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ReportsFactory  {

    private Map<String, NativeQueryReport<?>> reportBeans;
    
    @Autowired
    private ApplicationContext context;

    @SuppressWarnings("unchecked")
	public NativeQueryReport<T> getReportComponent(String reportName) {
        if (reportBeans == null) {
            loadReports();            
        }
        return (NativeQueryReport<T>) reportBeans.get(reportName);
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
