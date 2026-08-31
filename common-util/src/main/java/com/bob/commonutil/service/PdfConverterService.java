package com.bob.commonutil.service;

import com.bob.commonutil.util.AppConstants;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfConverterService {

    public byte[] convertHtmlStringToPdf(String html){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ConverterProperties props = new ConverterProperties();

        // Use classpath protocol directly (refer the constant below) - works in both development run and in JAR deployments
        props.setBaseUri(AppConstants.TEMPLATES_CLASSPATH_BASE_URI);
        props.setBaseUri(AppConstants.IMAGES_CLASSPATH_BASE_URI);

        HtmlConverter.convertToPdf(html, baos, props);
        return baos.toByteArray();
    }
}
