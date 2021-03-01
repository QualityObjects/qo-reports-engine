package com.qualityobjects.reports.common;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.qualityobjects.commons.exception.DataReadRuntimeException;

import org.springframework.core.io.ClassPathResource;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;

public class ClasspathImageResolver extends ITextUserAgent {
    public final String CLASSPATH_URI_PREFIX = "classpath:";

    public ClasspathImageResolver(ITextOutputDevice outputDevice, SharedContext sharedContext) {
        super(outputDevice);
        setSharedContext(sharedContext);
    }

    @Override
    public String resolveURI(String uri) {
        return uri;
    }

    @Override
    protected InputStream resolveAndOpenStream(String uri) {
        if (uri != null && uri.startsWith(CLASSPATH_URI_PREFIX)) {
            String resourcePath = uri.substring(CLASSPATH_URI_PREFIX.length());                
            InputStream is = getClass().getResourceAsStream(resourcePath);
            if (is == null) {
                throw new DataReadRuntimeException("Error loading pdf resource: " + uri);
            }
            return is;
        } else {
            return super.resolveAndOpenStream(uri);
        }
    }
}