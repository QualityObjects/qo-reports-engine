package com.qualityobjects.reports.service;

import java.lang.annotation.*;

/**
 * Anotación para componentes de tipo report
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Report {

	/**
     * Report name
     */ 
	String value();

}