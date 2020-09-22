package com.qualityobjects.reports.service;

import com.qualityobjects.commons.exception.GeneratingCSVErrorException;
import com.qualityobjects.commons.exception.QOException;
import com.qualityobjects.commons.exception.QORuntimeException;
import com.qualityobjects.reports.nativequery.Condition;
import com.qualityobjects.reports.nativequery.repo.NativeQueryRepository;
import com.qualityobjects.springboot.dto.PageData;
import com.qualityobjects.springboot.dto.PageParams;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.supercsv.encoder.DefaultCsvEncoder;
import org.supercsv.io.CsvBeanWriter;
import org.supercsv.io.ICsvBeanWriter;
import org.supercsv.prefs.CsvPreference;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Stream;

/**
 * Interface que deben implementar todos los reports
 */
public interface NativeQueryReport<T> {

	public NativeQueryRepository getRepository();
	public CsvConfig getCsvConfig();
	public Sort getDefaultSort();
	
	public Condition createCondition(MultiValueMap<String, String> filterParams);
	


	@SuppressWarnings("unchecked")
	private Class<T> getDomainType() {
		Class<?>[] klasses = GenericTypeResolver.resolveTypeArguments(getClass(), NativeQueryReport.class);

		if (klasses != null && klasses.length > 0) {
			return (Class<T>)klasses[0];
		} else {
			throw new QORuntimeException("No se pudo resolver el tipo Generic de la clase: " + getClass());
		}
	}

	/**
	 * Method to process data after the row is retrieved from DB.
	 * @param row
	 */
	public default T postProcessData(T row) throws QOException {
		return row;
	}
	
    public default PageData<T> getPage(Pageable pageable, Condition where, PageParams params) throws QOException {
        Page<T> page = getRepository().findAll(where, pageable, getDomainType());

        for (T ars : page) {
            postProcessData(ars);
        }
        return PageData.of(page,params);
    }

	public default void exportCSV(Condition where, PrintWriter writer) throws QOException {
		exportCSV(where, getDefaultSort(), writer);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public default void exportCSV(Condition where, Sort sort, PrintWriter writer) throws QOException {
		CsvConfig config = getCsvConfig();
		CsvPreference pref = new CsvPreference.Builder(config.getCsvPreference()).useEncoder(new DefaultCsvEncoder()).build();
		try (	Stream<T> stream = getRepository().findAllAsStream(where, sort, getDomainType());
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
