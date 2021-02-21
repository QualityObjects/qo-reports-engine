package com.qualityobjects.reports.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.qualityobjects.commons.exception.DataReadRuntimeException;
import com.qualityobjects.reports.common.ClasspathImageResolver;
import com.qualityobjects.reports.nativequery.Condition;
import com.qualityobjects.reports.service.base.NativeQueryReport;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

/**
 * Interface que deben implementar todos los reports con salida CSV
 */
public interface NativeQueryReportPdf<T> extends NativeQueryReport<T> {

	public String getHtmlPageTemplateResource();

	public default String getHtmlPageTemplateContent() {
		try {
			return Files.readString(Paths.get(getClass().getResource(this.getHtmlPageTemplateResource()).toURI()), Charset.forName("utf-8"));
		} catch (IOException | URISyntaxException e) {
			throw new DataReadRuntimeException("Error reading resource: " + this.getHtmlPageTemplateResource(), e);
		}
	}

	/**
	 * Get the parameters to apply on the template
	 * 
	 * @param iterator Iterator over the data
	 * @param ctx Context data that can contains info about previous page, last record, and so on.
	 * @return The parameters to applu on page template
	 */
	public Map<String, Object> getTemplateData(Iterator<T> iterator, Map<String, Object> ctx);

	/**
	 * it receives the data and the page tample (that usually is cached to be retrieved just one time in all the process)
	 * 
	 * @param iterator
	 * @param pageTemplate
	 * @return
	 */
	public default String renderPage(Iterator<T> iterator, String pageTemplate, Map<String, Object> ctx) {
		Map<String, Object> params = getTemplateData(iterator, ctx);
		StringSubstitutor ss = new StringSubstitutor(params);

		return ss.replace(pageTemplate);
	}

	@Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public default void generatePdf(Condition where, OutputStream output) {
		List<T> data = getRepository().findAll(where, this.getDefaultSort(), this.getDomainType());
        Iterator<T> iterator = data.iterator();
		Map<String, Object> ctx = new HashMap<>();
        try {
            final String pageTemplate = this.getHtmlPageTemplateContent();
            ITextRenderer renderer = new ITextRenderer();
            ClasspathImageResolver cir = new ClasspathImageResolver(renderer.getOutputDevice(), renderer.getSharedContext());
            renderer.getSharedContext().setUserAgentCallback(cir);
            boolean isFirstPage = true;
            while (iterator.hasNext()) {
                String renderedPage = this.renderPage(iterator, pageTemplate, ctx);
                renderer.setDocumentFromString(renderedPage);
                renderer.layout();
                
                if (isFirstPage) {
                    renderer.createPDF(output, false);
                    isFirstPage = false;
                } else {
                    renderer.writeNextDocument();
                }
                
                output.flush();
            }
            renderer.finishPDF();
            output.close();

        } catch (IOException e) {
            throw new DataReadRuntimeException("Error reading data to generate PDF", e);
        }

    }

}
