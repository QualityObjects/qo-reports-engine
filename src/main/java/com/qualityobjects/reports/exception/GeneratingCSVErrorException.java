package com.qualityobjects.reports.exception;

import com.qualityobjects.commons.exception.QOException;

public class GeneratingCSVErrorException extends QOException {

	private static final long serialVersionUID = 5207795630755628143L;

	public GeneratingCSVErrorException(String mensaje) {
		super(ErrorCodes.UNKNOWN, mensaje);
	}

}