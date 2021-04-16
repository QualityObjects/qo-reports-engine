package com.qualityobjects.reports.service.base;

import com.qualityobjects.reports.exception.GeneratingCSVErrorException;
import com.qualityobjects.commons.exception.QOException;
import com.qualityobjects.commons.exception.QORuntimeException;
import com.qualityobjects.springboot.repository.StreamSpecificationRepository;
import lombok.Data;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.supercsv.encoder.DefaultCsvEncoder;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Stream;

/**
 * Interface que deben implementar todos los servicios que realicen paginación de entidades 
 */
public interface StreamingData<T> {

	public StreamSpecificationRepository<T> getRepository();
	

	@SuppressWarnings("unchecked")
	private Class<T> getDomainType() {
		Class<?>[] klasses = GenericTypeResolver.resolveTypeArguments(getClass(), StreamingData.class);

		if (klasses != null && klasses.length > 0) {
			return (Class<T>)klasses[0];
		} else {
			throw new QORuntimeException("No se pudo resolver el tipo Generic de la clase: " + getClass());
		}
    }
	
	@Transactional(readOnly = true)
	public default void exportCSV(CSVConfig config, Specification<T> specs, Sort sort, PrintWriter writer) throws QOException, GeneratingCSVErrorException {
		CsvPreference pref = new CsvPreference.Builder(config.getCsvPreference()).useEncoder(new DefaultCsvEncoder()).build();
		try(Stream<T> list = getRepository().streamAll(specs, sort, getDomainType());
			ICsvBeanWriter beanWriter = new CsvBeanWriter(writer, pref)) {			
			if (config.getHeader() != null) {
				beanWriter.writeHeader(config.getHeader());
			}
			list.forEachOrdered(element -> {
				try {
					element = postProcessData(element);
					beanWriter.write(element, config.getProperties());
				} catch (IOException | QOException e) {					
					throw new QORuntimeException("Error generando CSV: " + e.toString() + " elemento: " + element);
				}
			});
		} catch (IOException e) {
			throw new GeneratingCSVErrorException("Error generando CSV: " + e.toString());
		}

	}

	/**
	 * Method to process data after the row is retrieved from DB.
	 * @param row
	 */
	public default T postProcessData(T row) throws QOException {
		return row;
	}
	
	@Data
	public static class CSVConfig {
		private String[] header;
		private String[] properties;
		private CsvPreference csvPreference = CsvPreference.EXCEL_NORTH_EUROPE_PREFERENCE;
	}
}
