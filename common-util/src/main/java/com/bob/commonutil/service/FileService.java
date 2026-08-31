package com.bob.commonutil.service;


import com.bob.commonutil.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.List;

/**
 * Service for file operations including remote and local folder creation and file upload.
 */
@Service
@Slf4j
public class FileService {

    @Value("${useAzureBlob:false}")
    private Boolean useAzureBlob;

    @Value("${remote.host.url}")
    private String remoteHostURL;

    @Value("${remote.host.user}")
    private String remoteHostUser;

    @Value("${remote.host.password}")
    private String remoteHostPassword;

    @Autowired
    AzureBlobStorageService azureBlobStorageService;


    public String uploadFile(MultipartFile file, String fileName, String uploadDir) throws IOException {
            return azureBlobStorageService.uploadFile(file,fileName,uploadDir);
        }


    public void deleteExistingFiles(String uploadDir, String baseFileName) {
        try {
            azureBlobStorageService.deleteFile(uploadDir, baseFileName);
            log.info("Attempted to delete blob  from container  in Azure Blob Storage.");
        } catch (Exception e) {
            log.error("Failed to delete existing blob for base name in Azure Blob Storage.");
            throw new CommonException("Failed to delete old blobs");
        }
    }


    /**
     * Extracts text content from a file, supporting PDF, DOC, and DOCX formats.
     *
     * @param filePath The absolute path to the file.
     * @return The extracted text content, or null if the file type is unsupported or an error occurs.
     */
    public String extractTextFromFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            log.warn("File path is null or empty.");
            return null;
        }

        String lowercasedFilePath = filePath.toLowerCase();
        if (lowercasedFilePath.endsWith(".pdf")) {
            return extractTextFromPdf(filePath);
        } else if (lowercasedFilePath.endsWith(".doc") || lowercasedFilePath.endsWith(".docx")) {
            return extractTextFromDoc(filePath);
        } else {
            log.warn("Unsupported file type");
            return null;
        }
    }

    private InputStream getInputStream(String filePath) throws IOException {
            byte[] blobBytes = azureBlobStorageService.downloadFile("", filePath);
            return new ByteArrayInputStream(blobBytes);
    }

    private String extractTextFromPdf(String filePath) {
        try (InputStream is = getInputStream(filePath);
             PDDocument document = PDDocument.load(is)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        } catch (IOException e) {
            log.error("Error extracting text from PDF file ");
            return null;
        }
    }

    private String extractTextFromDoc(String filePath) {
        String lowercasedFilePath = filePath.toLowerCase();
        try (InputStream is = getInputStream(filePath)) {
            if (lowercasedFilePath.endsWith(".docx")) {
                XWPFDocument docx = new XWPFDocument(is);
                List<XWPFParagraph> paragraphs = docx.getParagraphs();
                StringBuilder text = new StringBuilder();
                for (XWPFParagraph para : paragraphs) {
                    text.append(para.getText()).append("\n");
                }
                return text.toString();
            } else if (lowercasedFilePath.endsWith(".doc")) {
                HWPFDocument doc = new HWPFDocument(is);
                WordExtractor extractor = new WordExtractor(doc);
                return extractor.getText();
            } else {
                log.warn("Unsupported file type for DOC/DOCX extraction");
                return null;
            }
        } catch (IOException e) {
            log.error("Error extracting text from document file");
            return null;
        }
    }
}
