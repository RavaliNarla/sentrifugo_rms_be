package com.sentrifugo.rms.common.service;

import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfConverterService {

    public byte[] convertHtmlStringToPdf(String html) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ConverterProperties props = new ConverterProperties();
        HtmlConverter.convertToPdf(html, baos, props);
        return baos.toByteArray();
    }
}
