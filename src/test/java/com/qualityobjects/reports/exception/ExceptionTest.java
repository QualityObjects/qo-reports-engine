package com.qualityobjects.reports.exception;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.qualityobjects.commons.exception.QOException;
import com.qualityobjects.commons.exception.QOException.ErrorCodes;

public class ExceptionTest {
	
	@Test
	// (reports) - controller -> StreamingDataController / service -> StreamingData, NativeQueryReportCsv
	void generatingCSVErrorException() {
		assertThrows(GeneratingCSVErrorException.class, () -> {
			try {
				String mensaje = "Error generating CSV";
				throw new GeneratingCSVErrorException(mensaje);
			} catch (QOException ex) {
				assertEquals("Error generating CSV", ex.getMessage());
				assertEquals(418 /* UNKNOWN */, ex.getHttpStatus());
				assertEquals(ErrorCodes.UNKNOWN, ex.getCode());
				throw ex;
			}
		});
	}

}