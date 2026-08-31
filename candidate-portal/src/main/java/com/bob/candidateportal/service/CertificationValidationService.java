package com.bob.candidateportal.service;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.InvalidFileTypeException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.OcrService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.TempDirUtil;
import com.bob.db.entity.CertificationMasterEntity;
import com.bob.db.repository.CertificationMasterRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class CertificationValidationService {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private CertificationMasterRepository certificationMasterRepository;

    public boolean validateCertification(MultipartFile file, UUID certificationId) {
        try {
            // Fetch certification master data
            CertificationMasterEntity certification = certificationMasterRepository.findById(certificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Certification not found for id: " + certificationId));

            // Check if keywords are configured
            JsonNode keywordsNode = certification.getKeywords();
            
            // If no keywords configured (null or empty), pass validation (e.g., for "Other" certificates)
            if (keywordsNode == null || !keywordsNode.isArray() || keywordsNode.isEmpty()) {
                log.info("No keywords configured for certification: {}. Validation passed by default.", 
                    certification.getCertificationName());
                return true;
            }

            // Determine file type and validate accordingly
            return extractTextFromFile(file, keywordsNode, certification.getCertificationName());

        } catch (Exception e) {
            log.error("Error validating certification: {}", e.getMessage(), e);
            throw new CommonException("Error validating certification");
        }
    }

    private boolean extractTextFromFile(MultipartFile file, JsonNode keywordsNode, String certificationName) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        // Handle image files with OCR
        if (Arrays.asList(AppConstants.IMAGE_EXTENSIONS).contains(extension)) {
            log.debug("Processing image file with OCR: {}", filename);
            String ocrText = ocrService.extractTextFromFile(file);
            
            // Validate OCR text against keywords
            if (ocrText != null && !ocrText.trim().isEmpty()) {
                boolean isValid = validateKeywords(ocrText, keywordsNode, certificationName);
                if (isValid) {
                    log.info("Image validation successful - keywords found in OCR text");
                } else {
                    log.warn("Image validation failed - keywords not found in OCR text");
                }
                return isValid;
            } else {
                log.warn("OCR extracted no text from image");
                return false;
            }
        }
        
        // Handle document files (PDF, DOC, DOCX)
        else if (Arrays.asList(AppConstants.DOCUMENT_EXTENSIONS).contains(extension)) {
            log.debug("Processing document file: {}", filename);
            return extractTextFromDocument(file, keywordsNode, certificationName);
        }
        
        else {
            throw new InvalidFileTypeException(
                "Unsupported file format: " + extension.toUpperCase() + 
                ". Supported formats: JPG, JPEG, PNG, PDF, DOC, DOCX"
            );
        }
    }

    private boolean extractTextFromDocument(MultipartFile file, JsonNode keywordsNode, String certificationName) throws Exception {
        // Get filename before transferring
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }
        String lowercasedFilename = filename.toLowerCase();
        
        // Create our own temp file and transfer MultipartFile to it
        // This releases the original uploaded temp file so Spring can clean it up

        Path tempFilePath = null;
        try {
            // Create temp file with appropriate extension
            String extension = null;
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = filename.substring(dotIndex);
            }

            tempFilePath = TempDirUtil.createSecureTempFile("cert_validation_", extension);

            // Transfer uploaded file to our temp file (releases original file)
            file.transferTo(tempFilePath);
            
            // Now read our temp file into byte array for processing
            byte[] fileBytes = java.nio.file.Files.readAllBytes(tempFilePath);
            
            // Stage 1: Try text extraction first
            String extractedText;
            try (InputStream is = new java.io.ByteArrayInputStream(fileBytes)) {
                if (lowercasedFilename.endsWith(".pdf")) {
                    extractedText = extractTextFromPdf(is);
                } else if (lowercasedFilename.endsWith(AppConstants.DOCX)) {
                    extractedText = extractTextFromDocx(is);
                } else if (lowercasedFilename.endsWith(".doc")) {
                    extractedText = extractTextFromDoc(is);
                } else {
                    log.warn("Unsupported document type: {}", filename);
                    return false;
                }
            }
            
            // Check if we have meaningful text and if it contains keywords
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                // Try validating with the extracted text first
                if (validateKeywords(extractedText, keywordsNode, certificationName)) {
                    log.info("Validation successful with text extraction for: {}", filename);
                    return true;
                }
                log.info("Keywords not found in extracted text ({} chars). Will try OCR fallback.", 
                    extractedText.length());
            } else {
                log.info("No text extracted or text is empty. Will try OCR fallback.");
            }
            
            // Multi-stage OCR approach for all document types
            if (lowercasedFilename.endsWith(".pdf")) {
                return validatePdfWithMultiStageOcr(fileBytes, extractedText, keywordsNode, certificationName, filename);
            } else if (lowercasedFilename.endsWith(AppConstants.DOCX) || lowercasedFilename.endsWith(".doc")) {
                return validateDocxDocWithMultiStageOcr(fileBytes, extractedText, keywordsNode, certificationName, filename, lowercasedFilename);
            }
            
            log.warn("Unsupported document type for OCR: {}", filename);
            return false;
            
        } finally {
            TempDirUtil.deleteIfExists(tempFilePath);
        }
    }

    private String extractTextFromPdf(InputStream is) {
        try (PDDocument document = PDDocument.load(is)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        } catch (IOException e) {
            log.error("Error extracting text from PDF: {}", e.getMessage(), e);
            return null;
        }
    }

    private String extractTextFromDocx(InputStream is) {
        try (XWPFDocument docx = new XWPFDocument(is)) {
            List<XWPFParagraph> paragraphs = docx.getParagraphs();
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph para : paragraphs) {
                text.append(para.getText()).append("\n");
            }
            return text.toString();
        } catch (IOException e) {
            log.error("Error extracting text from DOCX: {}", e.getMessage(), e);
            return null;
        }
    }

    private String extractTextFromDoc(InputStream is) {
        try (HWPFDocument doc = new HWPFDocument(is);
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        } catch (IOException e) {
            log.error("Error extracting text from DOC: {}", e.getMessage(), e);
            return null;
        }
    }

    // OCR Fallback Methods
    
    private String extractTextFromDocumentImagesWithOcr(byte[] fileBytes, String lowercasedFilename) throws Exception {
        List<BufferedImage> images = new ArrayList<>();
        
        // Extract images based on document type
        if (lowercasedFilename.endsWith(".pdf")) {
            images = extractImagesFromPdf(fileBytes);
        } else if (lowercasedFilename.endsWith(AppConstants.DOCX)) {
            images = extractImagesFromDocx(fileBytes);
        } else if (lowercasedFilename.endsWith(".doc")) {
            images = extractImagesFromDoc(fileBytes);
        }
        
        if (images.isEmpty()) {
            log.warn("No images found in document");
            return null;
        }
        
        log.info("Found {} image(s) in document. Running OCR...", images.size());
        
        // Run OCR on each image and combine results
        StringBuilder combinedText = new StringBuilder();
        for (int i = 0; i < images.size(); i++) {
            BufferedImage image = images.get(i);
            String ocrText = runOcrOnImage(image, i);
            if (ocrText != null && !ocrText.trim().isEmpty()) {
                combinedText.append(ocrText).append("\n");
            }
        }
        
        String result = combinedText.toString().trim();
        log.info("OCR completed. Extracted {} characters from images", result.length());
        return result.isEmpty() ? null : result;
    }
    
    private List<BufferedImage> extractImagesFromPdf(byte[] fileBytes) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes);
             PDDocument document = PDDocument.load(is)) {
            
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            
            // Render each page as an image at 300 DPI for OCR
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                // Use 300 DPI for optimal OCR quality
                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 300);
                
                // Convert to grayscale for better OCR performance
                BufferedImage grayscaleImage = new BufferedImage(
                    pageImage.getWidth(), 
                    pageImage.getHeight(), 
                    BufferedImage.TYPE_BYTE_GRAY
                );
                java.awt.Graphics2D g = grayscaleImage.createGraphics();
                g.drawImage(pageImage, 0, 0, null);
                g.dispose();
                
                images.add(grayscaleImage);
            }
        }
        
        return images;
    }
    
    private List<BufferedImage> extractImagesFromDocx(byte[] fileBytes) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes);
             XWPFDocument docx = new XWPFDocument(is)) {
            
            List<XWPFPictureData> pictures = docx.getAllPictures();
            for (XWPFPictureData picture : pictures) {
                byte[] imageData = picture.getData();
                BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(imageData));
                if (image != null) {
                    images.add(image);
                }
            }
        }
        
        return images;
    }
    
    private List<BufferedImage> extractImagesFromDoc(byte[] fileBytes) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes);
             HWPFDocument doc = new HWPFDocument(is)) {
            
            List<Picture> pictures = doc.getPicturesTable().getAllPictures();
            for (Picture picture : pictures) {
                byte[] imageData = picture.getContent();
                BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(imageData));
                if (image != null) {
                    images.add(image);
                }
            }
        }
        
        return images;
    }
    
    private String runOcrOnImage(BufferedImage image, int imageIndex){
        Path tempFilePath = null;
        try {
            // Create temporary file for OCR processing
            tempFilePath = TempDirUtil.createSecureTempFile("ocr_image_" + imageIndex + "_", ".png");

            // Convert to grayscale for consistent OCR across platforms
            // OCR works better with grayscale - removes color noise
            BufferedImage grayscaleImage = new BufferedImage(
                image.getWidth(), 
                image.getHeight(), 
                BufferedImage.TYPE_BYTE_GRAY  // 8-bit grayscale (256 shades)
            );
            java.awt.Graphics2D g = grayscaleImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            
            // Use explicit PNG writer for consistent encoding across Windows/Linux
            javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
            javax.imageio.ImageWriteParam writeParam = writer.getDefaultWriteParam();
            javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(tempFilePath.toFile());
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(grayscaleImage, null, null), writeParam);
            ios.close();
            writer.dispose();
            
            // Load tessdata and run OCR directly using Tesseract
            File tessDataFolder = loadTessData();
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataFolder.getAbsolutePath());
            
            String ocrText = tesseract.doOCR(tempFilePath.toFile());
            log.debug("OCR on image {}: extracted {} characters", imageIndex, 
                ocrText != null ? ocrText.length() : 0);
            
            return ocrText;
        } catch (Exception e) {
            log.error("Error running OCR on image {}: {}", imageIndex, e.getMessage(), e);
            return null;
        } finally {
            TempDirUtil.deleteIfExists(tempFilePath);
        }
    }
    
    private File loadTessData() {
        File tempTessDataFolder = new File(System.getProperty("java.io.tmpdir"), "tessdata");
        if (!tempTessDataFolder.exists()) {
            boolean created = tempTessDataFolder.mkdirs();
            if (!created) {
                log.warn("Failed to create tessdata directory: {}", tempTessDataFolder.getAbsolutePath());
            }
        }
        
        String[] dataFiles = {"eng.traineddata"};
        
        for (String dataFile : dataFiles) {
            File targetFile = new File(tempTessDataFolder, dataFile);
            if (!targetFile.exists()) {
                try (InputStream inputStream = new ClassPathResource("tessdata/" + dataFile).getInputStream()) {
                    Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    log.error("Could not copy {} from resources: {}", dataFile, e.getMessage());
                }
            }
        }
        
        return tempTessDataFolder;
    }

    private boolean validateKeywords(String text, JsonNode keywordsNode, String certificationName) {
        // Normalize extracted text for robust cross-platform matching
        String normalizedText = normalizeTextForMatching(text);
        
        log.debug("Validating certification: {}", certificationName);
        log.debug("Original text length: {} characters", text.length());
        log.debug("Normalized text length: {} characters", normalizedText.length());
        
        // Add spaces around text for word boundary matching
        // This prevents substring matches like "ration" matching "enumeration"
        String textWithBoundaries = " " + normalizedText + " ";
        
        // Check if ANY keyword is present (OR logic with word boundaries)
        for (JsonNode keywordNode : keywordsNode) {
            String keyword = keywordNode.asText();
            String normalizedKeyword = normalizeTextForMatching(keyword);
            
            // Add spaces around keyword for whole-word/phrase matching
            String keywordWithBoundaries = " " + normalizedKeyword + " ";
            
            if (textWithBoundaries.contains(keywordWithBoundaries)) {
                log.info("Keyword matched: '{}' for certification: {}", keyword, certificationName);
                return true;
            }
        }
        
        log.warn("No keywords matched for certification: {}", certificationName);
        return false;
    }
    
    /**
     * Normalize text for robust keyword matching across different OCR outputs.
     * Handles platform-specific differences in character encoding, whitespace, and special characters.
     * 
     * @param text Text to normalize
     * @return Normalized text suitable for keyword matching
     */
    private String normalizeTextForMatching(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        return text
            // Convert to lowercase for case-insensitive matching
            .toLowerCase()
            // Normalize Unicode (NFD = decompose, then remove combining marks)
            .replaceAll("\\p{M}", "")
            // Replace all whitespace sequences (spaces, tabs, newlines, etc.) with single space
            .replaceAll("\\s+", " ")
            // Remove common OCR noise characters and special symbols
            .replaceAll("[^a-z0-9\\s]", "")
            // Remove extra spaces at start/end
            .trim();
    }
    
    // ===== Multi-Stage OCR Methods (same as DocumentValidationService) =====
    
    private boolean validatePdfWithMultiStageOcr(byte[] fileBytes, String extractedText,
                                                  JsonNode keywordsNode, String certificationName,
                                                  String filename) throws Exception {
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes);
             PDDocument document = PDDocument.load(is)) {
            
            // Stage 1: Text extraction already attempted in calling method
            // If we're here, either no text was extracted or keywords were not found in extracted text
            
            // Stage 2: Try to extract embedded images first
            List<BufferedImage> embeddedImages = extractEmbeddedImagesFromPdf(document);
            
            if (!embeddedImages.isEmpty()) {
                log.info("Found {} embedded image(s) in PDF, running OCR on embedded images...", embeddedImages.size());
                
                // Run OCR on embedded images
                String embeddedOcrText = runOcrOnImages(embeddedImages);
                
                // Combine with extracted text and validate
                String combinedText = combineTexts(extractedText, embeddedOcrText);
                
                if (combinedText != null && !combinedText.trim().isEmpty()) {
                    if (validateKeywords(combinedText, keywordsNode, certificationName)) {
                        log.info("Validation successful with embedded images OCR for: {}", filename);
                        return true;
                    }
                    log.info("Keywords not found in embedded images OCR. Will try page rendering.");
                }
            } else {
                log.info("No embedded images found in PDF. Will try page rendering.");
            }
            
            // Stage 3: Render pages at 300 DPI as fallback
            log.info("Rendering PDF pages at 300 DPI for OCR...");
            List<BufferedImage> renderedImages = renderPdfPages(document);
            
            if (renderedImages.isEmpty()) {
                log.warn("No images could be rendered from PDF");
                return false;
            }
            
            // Run OCR on rendered pages
            String renderedOcrText = runOcrOnImages(renderedImages);
            
            // Combine with extracted text and validate
            String finalCombinedText = combineTexts(extractedText, renderedOcrText);
            
            if (finalCombinedText != null && !finalCombinedText.trim().isEmpty()) {
                boolean isValid = validateKeywords(finalCombinedText, keywordsNode, certificationName);
                if (isValid) {
                    log.info("Validation successful with page rendering OCR for: {}", filename);
                } else {
                    log.warn("Validation failed - keywords not found even after page rendering for: {}", filename);
                }
                return isValid;
            }
            
            log.warn("No text extracted from PDF OCR for: {}", filename);
            return false;
            
        } catch (IOException e) {
            log.error("Error processing PDF with multi-stage OCR: {}", e.getMessage(), e);
            throw new CommonException("Error processing PDF");
        }
    }
    
    private boolean validateDocxDocWithMultiStageOcr(byte[] fileBytes, String extractedText,
                                                      JsonNode keywordsNode, String certificationName,
                                                      String filename, String lowercasedFilename) throws Exception {
        // Extract embedded images from DOCX/DOC
        List<BufferedImage> embeddedImages;
        if (lowercasedFilename.endsWith(AppConstants.DOCX)) {
            embeddedImages = extractImagesFromDocx(fileBytes);
        } else {
            embeddedImages = extractImagesFromDoc(fileBytes);
        }
        
        if (embeddedImages.isEmpty()) {
            log.warn("No embedded images found in {} document: {}", lowercasedFilename.toUpperCase(), filename);
            return false;
        }
        
        log.info("Found {} embedded image(s) in {} document, running OCR...", embeddedImages.size(), lowercasedFilename.toUpperCase());
        
        // Run OCR on embedded images
        String embeddedOcrText = runOcrOnImages(embeddedImages);
        
        // Combine with extracted text and validate
        String combinedText = combineTexts(extractedText, embeddedOcrText);
        
        if (combinedText != null && !combinedText.trim().isEmpty()) {
            boolean isValid = validateKeywords(combinedText, keywordsNode, certificationName);
            if (isValid) {
                log.info("Validation successful with embedded images OCR for: {}", filename);
            } else {
                log.warn("Validation failed - keywords not found in embedded images OCR for: {}", filename);
            }
            return isValid;
        }
        
        log.warn("No text extracted from {} OCR for: {}", lowercasedFilename.toUpperCase(), filename);
        return false;
    }
    
    private List<BufferedImage> extractEmbeddedImagesFromPdf(PDDocument document) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        
        for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
            org.apache.pdfbox.pdmodel.PDPage page = document.getPage(pageIndex);
            org.apache.pdfbox.pdmodel.PDResources resources = page.getResources();
            
            if (resources != null) {
                for (org.apache.pdfbox.cos.COSName name : resources.getXObjectNames()) {
                    org.apache.pdfbox.pdmodel.graphics.PDXObject xobject = resources.getXObject(name);
                    
                    if (xobject instanceof org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) {
                        org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject image = 
                            (org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject) xobject;
                        BufferedImage bImage = image.getImage();
                        
                        if (bImage != null) {
                            log.debug("Extracted embedded image from page {} ({}x{})", 
                                pageIndex, bImage.getWidth(), bImage.getHeight());
                            images.add(bImage);
                        }
                    }
                }
            }
        }
        
        return images;
    }
    
    private List<BufferedImage> renderPdfPages(PDDocument document) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        
        for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
            BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 300);
            
            // Crop excessive whitespace from rendered pages
            BufferedImage croppedImage = cropWhitespace(pageImage);
            
            log.debug("Rendered page {} at 300 DPI ({}x{} -> {}x{} after cropping)", 
                pageIndex, pageImage.getWidth(), pageImage.getHeight(),
                croppedImage.getWidth(), croppedImage.getHeight());
            
            // Convert to grayscale for better OCR performance
            BufferedImage grayscaleImage = new BufferedImage(
                croppedImage.getWidth(), 
                croppedImage.getHeight(), 
                BufferedImage.TYPE_BYTE_GRAY
            );
            java.awt.Graphics2D g = grayscaleImage.createGraphics();
            g.drawImage(croppedImage, 0, 0, null);
            g.dispose();
            
            images.add(grayscaleImage);
        }
        
        return images;
    }
    
    private String runOcrOnImages(List<BufferedImage> images) throws IOException {
        if (images == null || images.isEmpty()) {
            return null;
        }
        
        StringBuilder combinedText = new StringBuilder();
        for (int i = 0; i < images.size(); i++) {
            BufferedImage image = images.get(i);
            String ocrText = runOcrOnImage(image, i);
            if (ocrText != null && !ocrText.trim().isEmpty()) {
                combinedText.append(ocrText).append("\n");
            }
        }

        String result = combinedText.toString().trim();
        return result.isEmpty() ? null : result;
    }
    
    private BufferedImage cropWhitespace(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Define threshold for "white" pixels
        int whiteThreshold = 240;
        
        int top = 0, bottom = height - 1, left = 0, right = width - 1;
        
        // Find top boundary
        topScan:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                if (red < whiteThreshold || green < whiteThreshold || blue < whiteThreshold) {
                    top = y;
                    break topScan;
                }
            }
        }
        
        // Find bottom boundary
        bottomScan:
        for (int y = height - 1; y >= top; y--) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                if (red < whiteThreshold || green < whiteThreshold || blue < whiteThreshold) {
                    bottom = y;
                    break bottomScan;
                }
            }
        }
        
        // Find left boundary
        leftScan:
        for (int x = 0; x < width; x++) {
            for (int y = top; y <= bottom; y++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                if (red < whiteThreshold || green < whiteThreshold || blue < whiteThreshold) {
                    left = x;
                    break leftScan;
                }
            }
        }
        
        // Find right boundary
        rightScan:
        for (int x = width - 1; x >= left; x--) {
            for (int y = top; y <= bottom; y++) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = rgb & 0xFF;
                
                if (red < whiteThreshold || green < whiteThreshold || blue < whiteThreshold) {
                    right = x;
                    break rightScan;
                }
            }
        }
        
        // Add padding
        int padding = Math.max(10, Math.min((right - left) / 20, (bottom - top) / 20));
        top = Math.max(0, top - padding);
        bottom = Math.min(height - 1, bottom + padding);
        left = Math.max(0, left - padding);
        right = Math.min(width - 1, right + padding);
        
        int newWidth = right - left + 1;
        int newHeight = bottom - top + 1;
        
        if (newWidth <= 0 || newHeight <= 0 || newWidth < width / 4 || newHeight < height / 4) {
            log.warn("Crop dimensions invalid or too small ({}x{}), using original image", newWidth, newHeight);
            return image;
        }
        
        return image.getSubimage(left, top, newWidth, newHeight);
    }
    
    private String combineTexts(String text1, String text2) {
        if (text1 != null && !text1.trim().isEmpty() && text2 != null && !text2.trim().isEmpty()) {
            return text1 + "\n" + text2;
        } else if (text2 != null && !text2.trim().isEmpty()) {
            return text2;
        } else {
            return text1;
        }
    }
}
