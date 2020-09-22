package com.qualityobjects.reports.controller;

import com.qualityobjects.commons.exception.InvalidInputDataException;
import com.qualityobjects.commons.exception.QOException;
import com.qualityobjects.reports.service.base.StreamingData;
import com.qualityobjects.reports.service.base.StreamingData.CSVConfig;
import com.qualityobjects.springboot.common.PaginationUtils;
import com.qualityobjects.springboot.controllers.SpecificationFilterGenerator;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface StreamingDataController<T> extends SpecificationFilterGenerator<T> {

	public StreamingData<T> getService();

	public PaginationUtils getPaginationUtils();

	@GetMapping(path = "/export/csv")
	public default void exportCSV(@Nullable @RequestParam("_sortFields") List<String> sortFields,
			@Nullable @RequestParam("_sortDir") Sort.Direction sortDirection,
			@Nullable @RequestParam("_header") List<String> header,
			@RequestParam("_properties") List<String> properties,
			@RequestParam MultiValueMap<String, String> filterParams, HttpServletResponse response)  throws QOException {
		String nameFile = "exportActivity.csv";
		try {
			response.setContentType("text/csv");
			response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s", nameFile));
			Specification<T> specs = this.getSpecificationFilter(filterParams);
			Sort sort = getPaginationUtils().generateSort(sortFields, sortDirection);
			CSVConfig config = new StreamingData.CSVConfig();
			if (header != null) {
				config.setHeader(header.toArray(new String[header.size()]));
			}
			config.setProperties(properties.toArray(new String[properties.size()]));
			
			this.getService().exportCSV(config, specs, sort, response.getWriter());
		} catch (IOException ex) {
			throw new InvalidInputDataException("Error producido al exportar el fichero: "+ ex.getMessage());
		}
	}

}
