package com.qualityobjects.reports.service;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Stream;

import com.qualityobjects.reports.exception.GeneratingCSVErrorException;
import com.qualityobjects.commons.exception.QOException;
import com.qualityobjects.commons.exception.QORuntimeException;
import com.qualityobjects.reports.nativequery.Condition;
import com.qualityobjects.reports.service.base.NativeQueryReport;

import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.encoder.DefaultCsvEncoder;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Interface que deben implementar todos los reports con salida CSV
 */
public interface NativeQueryReportCsv<T> extends NativeQueryReport<T> {

	public CsvConfig getCsvConfig();

	public default void exportCSV(Condition where, PrintWriter writer) throws QOException {
		exportCSV(where, getDefaultSort(), writer);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public default void exportCSV(Condition where, Sort sort, PrintWriter writer) throws QOException {
		CsvConfig config = getCsvConfig();
		CsvPreference pref = new CsvPreference.Builder(config.getCsvPreference()).useEncoder(new DefaultCsvEncoder()).build();
		try (	Stream<T> stream = getRepository().findAllAsStream(where, sort, this.getDomainType()); 
				ICsvBeanWriter beanWriter = new CsvBeanWriter(writer, pref) ) {
			if (config.getHeader() != null) {
				beanWriter.writeHeader(config.getHeader());
			}

			stream.forEachOrdered(element -> {
				try {
					beanWriter.write(postProcessData(element), config.getProperties());
				} catch (QOException | IOException e) {					
					throw new QORuntimeException("Error generando CSV: " + e.toString() + " elemento: " + element);
				}
			});
		} catch (IOException e) {
			throw new GeneratingCSVErrorException("Error generando CSV: " + e.toString());
		}

	}
	
	@RequiredArgsConstructor(staticName = "of")
	@Data
	public static class CsvConfig {
		private final String[] header;
		private final String[] properties;
		private final CsvPreference csvPreference;

		public static CsvConfig of(String[] header, String[] properties) {
			return new CsvConfig(header, properties, CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE);
		}
	}
}
