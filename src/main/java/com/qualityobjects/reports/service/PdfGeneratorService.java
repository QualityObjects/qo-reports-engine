package com.qualityobjects.reports.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import com.qualityobjects.commons.exception.DataReadRuntimeException;
import com.qualityobjects.reports.common.ClasspathImageResolver;
import com.qualityobjects.reports.service.base.PdfPageRenderer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.xhtmlrenderer.pdf.ITextRenderer;

@Service
public class PdfGeneratorService {

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public <T> void generatePdf(PdfPageRenderer<T> ppr, Iterable<T> data, OutputStream output) {
        Iterator<T> iterator = data.iterator();

        try {
            final String pageTemplate = ppr.getHtmlPageTemplateContent();
            ITextRenderer renderer = new ITextRenderer();
            ClasspathImageResolver cir = new ClasspathImageResolver(renderer.getOutputDevice(), renderer.getSharedContext());
            renderer.getSharedContext().setUserAgentCallback(cir);
            boolean isFirstPage = true;
            while (iterator.hasNext()) {
                String renderedPage = ppr.renderPage(iterator, pageTemplate);
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
        } finally {
            if (data instanceof AutoCloseable) {
                try {
                    AutoCloseable.class.cast(data).close();
                } catch (Exception e) {
                    // Ignored on purpose
                }
            }
        }

    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        System.out.println("URI in " + PdfGeneratorService.class.getResource("/qo_logo.png").toURI());
        
        final String pageTemplate = Files.readString(Paths.get(new URI("file:///home/rob/git/QO/HEMERA/qo-reports-engine/src/test/resources/pdf-page-template.html")), Charset.forName("utf-8"));
        ITextRenderer renderer = new ITextRenderer();
        ClasspathImageResolver cir = new ClasspathImageResolver(renderer.getOutputDevice(), renderer.getSharedContext());
        renderer.getSharedContext().setUserAgentCallback(cir);

        File f = new File("/mnt/d/test_output.pdf");
        
        OutputStream output = new FileOutputStream(f);
              
        renderer.setDocumentFromString(pageTemplate);
        renderer.layout();
        renderer.createPDF(output, false);
        output.flush();

        renderer.setDocumentFromString(pageTemplate);
        renderer.layout();
        renderer.writeNextDocument();
        output.flush();

        renderer.finishPDF();
        output.close();
        System.out.println("file generated in " + f);

    }
}
