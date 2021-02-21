package com.qualityobjects.reports.service.base;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

import com.qualityobjects.commons.exception.DataReadRuntimeException;

import org.apache.commons.text.StringSubstitutor;

/**
 * Interface for Pdf reports the will render a single page based on a page template and the params got from data
 */
public interface PdfPageRenderer<T> {

	public String getHtmlPageTemplateResource();

	public default String getHtmlPageTemplateContent() {
		try {
			return Files.readString(Paths.get(getClass().getResource(this.getHtmlPageTemplateResource()).toURI()), Charset.forName("utf-8"));
		} catch (IOException | URISyntaxException e) {
			throw new DataReadRuntimeException("Error reading resource: " + this.getHtmlPageTemplateResource(), e);
		}
	}

	public Map<String, Object> getTemplateData(Iterator<T> iterator);

	
	public T getLastRecord();

	/**
	 * it receives the data and the page tample (that usually is cached to be retrieved just one time in all the process)
	 * 
	 * @param iterator
	 * @param pageTemplate
	 * @return
	 */
	public default String renderPage(Iterator<T> iterator, String pageTemplate) {
		Map<String, Object> params = getTemplateData(iterator);
		StringSubstitutor ss = new StringSubstitutor(params);

		return ss.replace(pageTemplate);
	}
	
}
