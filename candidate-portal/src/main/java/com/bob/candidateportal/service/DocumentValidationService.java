package com.bob.candidateportal.service;

import com.bob.candidateportal.model.DocumentValidationResult;
import com.bob.candidateportal.model.WorkExpFieldValidationResult;
import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.InvalidFileTypeException;
import com.bob.commonutil.exception.ResourceNotFoundException;
import com.bob.commonutil.service.OcrService;
import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.TempDirUtil;
import com.bob.db.entity.DocumentTypesEntity;
import com.bob.db.repository.DocumentTypesRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
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
public class DocumentValidationService {

    @Autowired
    private OcrService ocrService;

    @Autowired
    private DocumentTypesRepository documentTypesRepository;



    @Autowired
    private ObjectMapper objectMapper;

    public boolean validateCertificate(MultipartFile file, String certType) {
        try {
            DocumentTypesEntity documentType = documentTypesRepository.findByDocCode(certType);
            if (documentType == null) {
                throw new ResourceNotFoundException(AppConstants.DOCUMENT_TYPE_NOT_FOUND_MESSAGE);
            }

            String text = ocrService.extractTextFromFile(file);
//            System.out.println("Extracted text: " + text);

            if (text != null) {
                String lowerCaseText = text.toLowerCase();
                JsonNode keywordsNode = documentType.getKeywords();
                
                if (keywordsNode != null && keywordsNode.isArray()) {
                    for (JsonNode keywordNode : keywordsNode) {
                        String keyword = keywordNode.asText().toLowerCase();
                        if (lowerCaseText.contains(keyword)) {
                            return true;
                        }
                    }
                }
            }
            
            return false;
        } catch (Exception e) {
//            e.printStackTrace();
            // Depending on requirements, you might want to rethrow or return false
            throw new CommonException("Error validating certificate.");
        }
    }

    // NEW ENHANCED VALIDATION METHOD WITH TEXT EXTRACTION + OCR FALLBACK
    
    public DocumentValidationResult validateIdProofDocument(MultipartFile file, UUID documentId, String documentNumber) {
        try {
            // Fetch document type data by id
            DocumentTypesEntity documentType = documentTypesRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException(AppConstants.DOCUMENT_TYPE_NOT_FOUND_MESSAGE));

            // Check if keywords are configured
            JsonNode keywordsNode = documentType.getKeywords();
            
            // If no keywords configured (null or empty), only validate document number
            if (keywordsNode == null || !keywordsNode.isArray() || keywordsNode.isEmpty()) {
                log.info("No keywords configured for document type: {}. Will only validate document number.", 
                    documentType.getDocumentName());
                boolean numberFound = validateDocumentNumberOnly(file, documentNumber, documentType.getDocumentName());
                return numberFound 
                    ? new DocumentValidationResult(AppConstants.VALIDATION_STATUS_VALIDATED, null)
                    : new DocumentValidationResult(AppConstants.VALIDATION_STATUS_PENDING, List.of(AppConstants. DOCUMENT_NUMBER));
            }

            // Validate both document number AND keywords
            return checkDocumentNumberAndKeywords(file, documentNumber, keywordsNode, documentType.getDocumentName());

        } catch (Exception e) {
            log.error("Error validating ID proof document: {}", e.getMessage(), e);
            throw new CommonException("Error validating ID proof document");
        }
    }

    private boolean validateDocumentNumberOnly(MultipartFile file, String documentNumber, String documentName) throws TesseractException, IOException {
        // Extract text from file (reuses existing logic)
        String extractedText = extractAllTextFromFile(file, documentName);
        
        if (extractedText == null || extractedText.trim().isEmpty()) {
            log.warn("No text extracted from file for document number validation");
            return false;
        }
        
        // Normalize and check for document number
        String normalizedText = normalizeTextForMatching(extractedText);
        String normalizedDocNumber = normalizeTextForMatching(documentNumber);
        
        String textWithBoundaries = " " + normalizedText + " ";
        String docNumberWithBoundaries = " " + normalizedDocNumber + " ";
        
        boolean found = textWithBoundaries.contains(docNumberWithBoundaries);
        if (found) {
            log.info("Document number '{}' found in document: {}", documentNumber, documentName);
        } else {
            log.warn("Document number '{}' NOT found in document: {}", documentNumber, documentName);
        }
        return found;
    }

    private DocumentValidationResult checkDocumentNumberAndKeywords(MultipartFile file, String documentNumber, 
                                                       JsonNode keywordsNode, String documentName) throws TesseractException, IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        // Handle image files with OCR
        if (Arrays.asList(AppConstants.IMAGE_EXTENSIONS).contains(extension)) {
            log.debug("Processing image file with OCR: {}", filename);
            String ocrText = ocrService.extractTextFromFile(file);
            
            if (ocrText == null || ocrText.trim().isEmpty()) {
                log.warn(AppConstants.OCR_TEXT_NOT_EXTRACTED_MESSAGE);
                return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_REJECTED, List.of(AppConstants.Document));
            }
            
            return checkDocumentNumberAndKeywords(ocrText, documentNumber, keywordsNode, documentName);
        }
        
        // Handle document files (PDF, DOC, DOCX) with conditional OCR fallback
        else if (Arrays.asList(AppConstants.DOCUMENT_EXTENSIONS).contains(extension)) {
            log.debug("Processing document file: {}", filename);
            return validateDocumentFileWithFallback(file, documentNumber, keywordsNode, documentName);
        }
        
        else {
            throw new InvalidFileTypeException(
                "Unsupported file format: " + extension.toUpperCase() + 
                ". Supported formats: JPG, PNG, TIFF, BMP, GIF, PDF, DOC, DOCX"
            );
        }
    }

    private DocumentValidationResult validateDocumentFileWithFallback(MultipartFile file, String documentNumber,
                                                      JsonNode keywordsNode, String documentName) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }
        String lowercasedFilename = filename.toLowerCase();
        
        Path tempFilePath = null;
        try {
            String extension = null;
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = filename.substring(dotIndex);
            }

            tempFilePath = TempDirUtil.createSecureTempFile("idproof_validation_", extension);

            file.transferTo(tempFilePath);
            byte[] fileBytes = Files.readAllBytes(tempFilePath);
            
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
                return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_REJECTED, List.of(AppConstants.Document));
                }
            }
            
            // Stage 1: Check if extracted text has both document number AND keywords
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                DocumentValidationResult result = checkDocumentNumberAndKeywords(extractedText, documentNumber, keywordsNode, documentName);
                if (AppConstants.VALIDATION_STATUS_VALIDATED.equals(result.getStatus())) {
                    log.info("Validation successful with text extraction for: {}", filename);
                    return result;
                }
                log.info("Document number or keywords not found in extracted text ({} chars). Will try OCR fallback.", 
                    extractedText.length());
            } else {
                log.info(AppConstants.OCR_TEXT_NOT_EXTRACTED_FALLBACK_MESSAGE);
            }
            
            // Stage 2 and Stage 3: Multi-stage OCR approach for all document types
            if (lowercasedFilename.endsWith(".pdf")) {
                return validatePdfWithMultiStageOcr(fileBytes, extractedText, documentNumber, keywordsNode, documentName, filename);
            } else if (lowercasedFilename.endsWith(AppConstants.DOCX) || lowercasedFilename.endsWith(".doc")) {
                return validateDocxDocWithMultiStageOcr(fileBytes, extractedText, documentNumber, keywordsNode, documentName, filename, lowercasedFilename);
            }
            
            log.warn("Unsupported document type for OCR: {}", filename);
            return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_REJECTED, List.of(AppConstants.Document));
            
        } finally {
            TempDirUtil.deleteIfExists(tempFilePath);
        }
    }

    private DocumentValidationResult checkDocumentNumberAndKeywords(String text, String documentNumber, 
                                                    JsonNode keywordsNode, String documentName) {
        // Normalize text once for both checks
        String normalizedText = normalizeTextForMatching(text);
        String textWithBoundaries = " " + normalizedText + " ";
        
        // Step 1: Check if document number is present (MANDATORY)
        // Remove all spaces from incoming document number and create flexible pattern
        String normalizedDocNumber = normalizeTextForMatching(documentNumber).replaceAll("\\s+", "");
        
        // Create regex pattern: MH12AB45CD -> m\\s?h\\s?1\\s?2\\s?a\\s?b\\s?4\\s?5\\s?c\\s?d
        // This matches: MH12AB45CD, MH12 AB45CD, MH 12 AB 45 CD, etc.
        StringBuilder patternBuilder = new StringBuilder("\\b");
        for (int i = 0; i < normalizedDocNumber.length(); i++) {
            patternBuilder.append(java.util.regex.Pattern.quote(String.valueOf(normalizedDocNumber.charAt(i))));
            if (i < normalizedDocNumber.length() - 1) {
                patternBuilder.append("\\s?"); // 0 or 1 space between characters
            }
        }
        patternBuilder.append("\\b");
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternBuilder.toString());
            if (!pattern.matcher(normalizedText).find()) {
            log.warn("Document number '{}' NOT found in text (flexible space matching)", documentNumber);
            
            // Check if keywords are present - if yes, only document number is missing
            for (JsonNode keywordNode : keywordsNode) {
                String keyword = keywordNode.asText();
                String normalizedKeyword = normalizeTextForMatching(keyword);
                String keywordWithBoundaries = " " + normalizedKeyword + " ";
                
                if (textWithBoundaries.contains(keywordWithBoundaries)) {
                    log.info("Keyword matched but document number not found");
                    return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_PENDING, List.of(AppConstants. DOCUMENT_NUMBER));
                }
            }
            
            // Neither document number nor keywords found
            return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_REJECTED, List.of(AppConstants.Document));
        }
        
        log.info("Document number '{}' found in text (matched with flexible spaces)", documentNumber);
        
        // Step 2: Check if ANY keyword is present (OR logic)
        for (JsonNode keywordNode : keywordsNode) {
            String keyword = keywordNode.asText();
            String normalizedKeyword = normalizeTextForMatching(keyword);
            String keywordWithBoundaries = " " + normalizedKeyword + " ";
            
            if (textWithBoundaries.contains(keywordWithBoundaries)) {
                log.info("Keyword matched: '{}' for document type: {}", keyword, documentName);
                log.info("ID proof validation successful - both document number and keyword found");
                return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_VALIDATED, null);
            }
        }
        
        log.warn("Document number found but no keywords matched for document type: {}", documentName);
        return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_PENDING, List.of(AppConstants. DOCUMENT_NUMBER));
    }
    
    /**
     * Multi-stage OCR validation for PDFs:
     * 1. Try embedded images → OCR → validate
     * 2. If embedded images found but validation failed → try page rendering → OCR → validate
     * 3. If no embedded images → try page rendering → OCR → validate
     */
    private DocumentValidationResult validatePdfWithMultiStageOcr(byte[] fileBytes, String extractedText, 
                                                  String documentNumber, JsonNode keywordsNode, 
                                                  String documentName, String filename){
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes);
             PDDocument document = PDDocument.load(is)) {
            
            // Stage 1: Text extraction already attempted in calling method
            // If we're here, either no text was extracted or document number/keywords were not found in extracted text
            
            // Stage 2: Try to extract embedded images first
            List<BufferedImage> embeddedImages = extractEmbeddedImagesFromPdf(document);
            
            if (!embeddedImages.isEmpty()) {
                log.info("Found {} embedded image(s) in PDF, running OCR on embedded images...", embeddedImages.size());
                
                // Run OCR on embedded images
                String embeddedOcrText = runOcrOnImages(embeddedImages);
                
                // Combine with extracted text and validate
                String combinedText = combineTexts(extractedText, embeddedOcrText);
                
                if (combinedText != null && !combinedText.trim().isEmpty()) {
                    DocumentValidationResult result = checkDocumentNumberAndKeywords(combinedText, documentNumber, keywordsNode, documentName);
                    if (AppConstants.VALIDATION_STATUS_VALIDATED.equals(result.getStatus())) {
                        log.info("Validation successful with embedded images OCR for: {}", filename);
                        return result;
                    }
                    log.info("Document number or keywords not found in embedded images OCR. Will try page rendering.");
                }
            } else {
                log.info(AppConstants.NO_EMBEDDED_IMAGES_FOUND_IN_PDF);
            }
            
            // Stage 3: Render pages at 300 DPI as fallback
            log.info("Rendering PDF pages at 300 DPI for OCR...");
            List<BufferedImage> renderedImages = renderPdfPagesForIdProof(document);
            
            if (renderedImages.isEmpty()) {
                log.warn(AppConstants.PDF_NO_RENDERABLE_IMAGES);
                return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_REJECTED, List.of(AppConstants.Document));
            }
            
            // Run OCR on rendered pages
            String renderedOcrText = runOcrOnImages(renderedImages);
            
            // Combine with extracted text and validate
            String finalCombinedText = combineTexts(extractedText, renderedOcrText);
            
            if (finalCombinedText != null && !finalCombinedText.trim().isEmpty()) {
                DocumentValidationResult result = checkDocumentNumberAndKeywords(finalCombinedText, documentNumber, keywordsNode, documentName);
                if (AppConstants.VALIDATION_STATUS_VALIDATED.equals(result.getStatus())) {
                    log.info("Validation successful with page rendering OCR for: {}", filename);
                } else {
                    log.warn("Validation failed - document number or keywords not found even after page rendering for: {}", filename);
                }
                return result;
            }
            
            log.warn("No text extracted from PDF OCR for: {}", filename);
            return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_REJECTED, List.of(AppConstants.Document));
            
        } catch (IOException e) {
            log.error("Error processing PDF with multi-stage OCR: {}", e.getMessage(), e);
            throw new CommonException("Error processing PDF");
        }
    }
    
    /**
     * Multi-stage OCR validation for DOCX/DOC files:
     * 1. Try embedded images → OCR → validate
     * 2. If embedded images found but validation failed → return false (no page rendering for DOCX/DOC)
     * 3. If no embedded images → return false
     */
    private DocumentValidationResult validateDocxDocWithMultiStageOcr(byte[] fileBytes, String extractedText,
                                                      String documentNumber, JsonNode keywordsNode,
                                                      String documentName, String filename, String lowercasedFilename) throws IOException {
        // Extract embedded images from DOCX/DOC
        List<BufferedImage> embeddedImages;
        if (lowercasedFilename.endsWith(AppConstants.DOCX)) {
            embeddedImages = extractImagesFromDocx(fileBytes);
        } else {
            embeddedImages = extractImagesFromDoc(fileBytes);
        }
        
        if (embeddedImages.isEmpty()) {
            log.warn("No embedded images found in {} document: {}", lowercasedFilename.toUpperCase(), filename);
            return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_REJECTED, List.of(AppConstants.Document));
        }
        
        log.info("Found {} embedded image(s) in {} document, running OCR...", embeddedImages.size(), lowercasedFilename.toUpperCase());
        
        // Run OCR on embedded images
        String embeddedOcrText = runOcrOnImages(embeddedImages);
        
        // Combine with extracted text and validate
        String combinedText = combineTexts(extractedText, embeddedOcrText);
        
        if (combinedText != null && !combinedText.trim().isEmpty()) {
            DocumentValidationResult result = checkDocumentNumberAndKeywords(combinedText, documentNumber, keywordsNode, documentName);
            if (AppConstants.VALIDATION_STATUS_VALIDATED.equals(result.getStatus())) {
                log.info("Validation successful with embedded images OCR for: {}", filename);
            } else {
                log.warn("Validation failed - document number or keywords not found in embedded images OCR for: {}", filename);
            }
            return result;
        }
        
        log.warn("No text extracted from {} OCR for: {}", lowercasedFilename.toUpperCase(), filename);
        return new DocumentValidationResult(AppConstants.VALIDATION_STATUS_REJECTED, List.of(AppConstants.Document));
    }
    
    /**
     * Multi-stage OCR validation for PDFs (keywords only, no document number check)
     * Used by validate-document API
     */
    private boolean validatePdfWithMultiStageOcrKeywordsOnly(byte[] fileBytes, String extractedText,
                                                              JsonNode keywordsNode, String documentName,
                                                              String filename){
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
                    if (validateKeywords(combinedText, keywordsNode, documentName)) {
                        log.info("Validation successful with embedded images OCR for: {}", filename);
                        return true;
                    }
                    log.info("Keywords not found in embedded images OCR. Will try page rendering.");
                }
            } else {
                log.info(AppConstants.NO_EMBEDDED_IMAGES_FOUND_IN_PDF);
            }
            
            // Stage 3: Render pages at 300 DPI as fallback
            log.info("Rendering PDF pages at 300 DPI for OCR...");
            List<BufferedImage> renderedImages = renderPdfPagesForIdProof(document);
            
            if (renderedImages.isEmpty()) {
                log.warn(AppConstants.PDF_NO_RENDERABLE_IMAGES);
                return false;
            }
            
            // Run OCR on rendered pages
            String renderedOcrText = runOcrOnImages(renderedImages);
            
            // Combine with extracted text and validate
            String finalCombinedText = combineTexts(extractedText, renderedOcrText);
            
            if (finalCombinedText != null && !finalCombinedText.trim().isEmpty()) {
                boolean isValid = validateKeywords(finalCombinedText, keywordsNode, documentName);
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
    
    /**
     * Multi-stage OCR validation for DOCX/DOC (keywords only, no document number check)
     * Used by validate-document API
     */
    private boolean validateDocxDocWithMultiStageOcrKeywordsOnly(byte[] fileBytes, String extractedText,
                                                                   JsonNode keywordsNode, String documentName,
                                                                   String filename, String lowercasedFilename) throws IOException {
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
            boolean isValid = validateKeywords(combinedText, keywordsNode, documentName);
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
    
    private String combineTexts(String text1, String text2) {
        if (text1 != null && !text1.trim().isEmpty() && text2 != null && !text2.trim().isEmpty()) {
            return text1 + "\n" + text2;
        } else if (text2 != null && !text2.trim().isEmpty()) {
            return text2;
        } else {
            return text1;
        }
    }

    // ===== BIRTHDATE PROOF VALIDATION HELPER METHODS =====
    
    /**
     * Check if date and keywords are present in the text.
     * Returns: "validated" (both match), "pending" (only keywords match), "rejected" (keywords don't match)
     */
    private String checkDateOfBirthAndKeywords(String text, String dateOfBirth, String fullNameAsPerCertificate,
                                                  JsonNode keywordsNode, String documentName) {
        // Step 1: Check if date is present
        boolean dateFound = checkDateInText(text, dateOfBirth, documentName);
        
        if (dateFound) {
            log.info("Date '{}' found in text (full name: {}, note: name not validated due to spelling ambiguity)", 
                dateOfBirth, fullNameAsPerCertificate);
        } else {
            log.warn("Date '{}' NOT found in text (full name: {})", dateOfBirth, fullNameAsPerCertificate);
        }
        
        // Step 2: Check if ANY keyword is present (OR logic) - normalize for keyword matching
        String normalizedText = normalizeTextForMatching(text);
        String textWithBoundaries = " " + normalizedText + " ";
        
        boolean keywordFound = false;
        for (JsonNode keywordNode : keywordsNode) {
            String keyword = keywordNode.asText();
            String normalizedKeyword = normalizeTextForMatching(keyword);
            String keywordWithBoundaries = " " + normalizedKeyword + " ";
            
            if (textWithBoundaries.contains(keywordWithBoundaries)) {
                log.info("Keyword matched: '{}' for document type: {} (Document name: {})", keyword, documentName, documentName);
                keywordFound = true;
                break;
            }
        }
        
        // Determine status based on what matched
        // Note: This method still returns String for backward compatibility
        // The actual pendingChecks logic is in validateBirthdateProofDocument method
        if (dateFound && keywordFound) {
            log.info("Birthdate proof validation: VALIDATED - both date and keyword found");
            return AppConstants.VALIDATION_STATUS_VALIDATED;
        } else if (!dateFound && keywordFound) {
            log.info("Birthdate proof validation: PENDING - keyword found but date not found");
            return AppConstants.VALIDATION_STATUS_PENDING;
        } else {
            log.warn("Birthdate proof validation: REJECTED - keyword not found");
            return AppConstants.VALIDATION_STATUS_REJECTED;
        }
    }
    
    /**
     * Check if date is present in text using flexible format matching.
     * Handles various date formats: DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY, DD Month YYYY, Month DD YYYY, etc.
     * Must be called BEFORE text normalization to preserve date format characters.
     */
    private boolean checkDateInText(String text, String dateOfBirth, String documentName) {
        if (text == null || text.isEmpty() || dateOfBirth == null || dateOfBirth.isEmpty()) {
            return false;
        }
        
        // Parse the input date (expected format: dd/mm/yyyy)
        String[] dateParts = dateOfBirth.split("/");
        if (dateParts.length != 3) {
            log.error("Invalid date format. Expected dd/mm/yyyy, got: {}", dateOfBirth);
            return false;
        }
        
        String day = dateParts[0].trim();
        String month = dateParts[1].trim();
        String year = dateParts[2].trim();
        
        int dayNum = Integer.parseInt(day);
        int monthNum = Integer.parseInt(month);
        
        // Convert to lowercase for case-insensitive matching
        String textLower = text.toLowerCase();
        
        // Get month names
        String monthNameFull = AppConstants.MONTH_NAMES_FULL[monthNum - 1];
        String monthNameShort = AppConstants.MONTH_NAMES_SHORT[monthNum - 1];
        
        log.debug("Searching for date: day={}, month={} ({}|{}), year={}", day, month, monthNameFull, monthNameShort, year);
        
        // Generate all possible date patterns
        List<String> patterns = generateDatePatterns(day, dayNum, month, monthNum, monthNameFull, monthNameShort, year);
        
        // Check if any pattern matches
        for (String pattern : patterns) {
            if (textLower.contains(pattern.toLowerCase())) {
                log.info("Date matched with pattern: '{}' in document: {}", pattern, documentName);
                return true;
            }
        }
        
        log.debug("Date not found in any expected format");
        return false;
    }
    
    /**
     * Generate all possible date format patterns for flexible matching.
     * Examples: 12/10/1996, 12-10-1996, 12.10.1996, 12 Oct 1996, Oct 12 1996, 12th October 1996, etc.
     */
    private List<String> generateDatePatterns(String day, int dayNum, String month, int monthNum, 
                                                String monthNameFull, String monthNameShort, String year) {
        List<String> patterns = new ArrayList<>();
        
        // Day with ordinal suffix (1st, 2nd, 3rd, 4th, etc.)
        String dayWithOrdinal = getDayWithOrdinal(dayNum);
        
        // Common separators
        String[] separators = {"/", "-", ".", " ", ", ", " of "};
        
        // Pattern 1: Numeric formats (DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY, DD MM YYYY, DD, MM, YYYY)
        for (String sep : separators) {
            patterns.add(day + sep + month + sep + year);          // 12/10/1996
            patterns.add(day + sep + String.format("%02d", monthNum) + sep + year); // 12/10/1996 with zero-padded month
            patterns.add(month + sep + day + sep + year);          // 10/12/1996 (MM/DD/YYYY)
            patterns.add(String.format("%02d", monthNum) + sep + day + sep + year);
        }
        
        // Pattern 2: Day Month Year (12 Oct 1996, 12 October 1996, 12th Oct 1996, 12th October 1996)
        patterns.add(day + " " + monthNameShort + " " + year);           // 12 Oct 1996
        patterns.add(day + " " + monthNameFull + " " + year);            // 12 October 1996
        patterns.add(dayWithOrdinal + " " + monthNameShort + " " + year); // 12th Oct 1996
        patterns.add(dayWithOrdinal + " " + monthNameFull + " " + year);  // 12th October 1996
        patterns.add(day + " " + monthNameShort + ", " + year);          // 12 Oct, 1996
        patterns.add(day + " " + monthNameFull + ", " + year);           // 12 October, 1996
        patterns.add(dayWithOrdinal + " of " + monthNameFull + " " + year); // 12th of October 1996
        
        // Pattern 3: Month Day Year (Oct 12 1996, October 12 1996, Oct 12th 1996, October 12th 1996)
        patterns.add(monthNameShort + " " + day + " " + year);           // Oct 12 1996
        patterns.add(monthNameFull + " " + day + " " + year);            // October 12 1996
        patterns.add(monthNameShort + " " + dayWithOrdinal + " " + year); // Oct 12th 1996
        patterns.add(monthNameFull + " " + dayWithOrdinal + " " + year);  // October 12th 1996
        patterns.add(monthNameShort + " " + day + ", " + year);          // Oct 12, 1996
        patterns.add(monthNameFull + " " + day + ", " + year);           // October 12, 1996
        
        // Pattern 4: With "th", "st", "nd", "rd" separated by space (12 th Oct 1996)
        patterns.add(day + " th " + monthNameShort + " " + year);
        patterns.add(day + " th " + monthNameFull + " " + year);
        patterns.add(day + " st " + monthNameShort + " " + year);
        patterns.add(day + " st " + monthNameFull + " " + year);
        patterns.add(day + " nd " + monthNameShort + " " + year);
        patterns.add(day + " nd " + monthNameFull + " " + year);
        patterns.add(day + " rd " + monthNameShort + " " + year);
        patterns.add(day + " rd " + monthNameFull + " " + year);
        
        return patterns;
    }
    
    /**
     * Get day with ordinal suffix (1st, 2nd, 3rd, 4th, etc.)
     */
    private String getDayWithOrdinal(int day) {
        if (day >= 11 && day <= 13) {
            return day + "th";
        }
        switch (day % 10) {
            case 1: return day + "st";
            case 2: return day + "nd";
            case 3: return day + "rd";
            default: return day + "th";
        }
    }
    
    // ===== WORK EXPERIENCE VALIDATION HELPER METHODS =====
    
    /**
     * Check if work experience fields and keywords are present in text.
     * Returns: "validated" (both match), "pending" (only keywords match), "rejected" (keywords don't match)
     */
    private String checkWorkExpFieldsAndKeywords(String text, String organization, String role, String postHeld,
                                                    String fromDate, String toDate, boolean isPresentlyWorking,
                                                    JsonNode keywordsNode, String documentName) {
        // First check all required fields (before normalization for dates)
        WorkExpFieldValidationResult fieldResult = checkWorkExpFieldsInText(text, organization, role, postHeld, fromDate, toDate, isPresentlyWorking, documentName);
        
        if (fieldResult.isFieldsFound()) {
            log.info("All required work experience fields found in text");
        } else {
            log.warn("Required work experience fields NOT found in text: {}", fieldResult.getMissingFields());
        }
        
        // Then check keywords (after normalization)
        String normalizedText = normalizeTextForMatching(text);
        String textWithBoundaries = " " + normalizedText + " ";
        
        boolean keywordFound = false;
        for (JsonNode keywordNode : keywordsNode) {
            String keyword = keywordNode.asText();
            String normalizedKeyword = normalizeTextForMatching(keyword);
            String keywordWithBoundaries = " " + normalizedKeyword + " ";
            
            if (textWithBoundaries.contains(keywordWithBoundaries)) {
                log.info("Keyword matched: '{}' for document type: {} (Document name: {})", keyword, documentName, documentName);
                keywordFound = true;
                break;
            }
        }
        
        // Determine status based on what matched
        if (fieldResult.isFieldsFound() && keywordFound) {
            log.info("Work experience validation: VALIDATED - all required fields and keyword found");
            return AppConstants.VALIDATION_STATUS_VALIDATED; // No pendingChecks needed for success
        } else if (keywordFound && !fieldResult.isFieldsFound()) {
            log.info("Work experience validation: PENDING - keyword found but required fields not found");
            // Return status with comma-joined missing fields
            return "pending:" + String.join(",", fieldResult.getMissingFields());
        } else {
            log.warn("Work experience validation: REJECTED - invalid document (keywords not found)");
            return "rejected:document";
        }
    }
    
    /**
     * Check if work experience fields are present in text.
     * TEMPORARY: Date validation commented out
     * If isPresentlyWorking: organization only
     * If not: organization AND (role OR postHeld)
     * 
     * FUTURE: Will validate dates as well
     * If isPresentlyWorking: organization AND fromDate
     * If not: organization AND (role OR postHeld) AND fromDate AND toDate
     */
    private WorkExpFieldValidationResult checkWorkExpFieldsInText(String text, String organization, String role, String postHeld,
                                              String fromDate, String toDate, boolean isPresentlyWorking,
                                              String documentName) {
        if (text == null || text.isEmpty()) {
            return new WorkExpFieldValidationResult(false, List.of(AppConstants.Document));
        }
        
        List<String> missingFields = new ArrayList<>();
        
        // Normalize text for organization, role, postHeld matching
        String normalizedText = normalizeTextForMatching(text);
        String textWithBoundaries = " " + normalizedText + " ";
        
        // Step 1: Check organization (MANDATORY)
        String normalizedOrg = normalizeTextForMatching(organization);
        String orgWithBoundaries = " " + normalizedOrg + " ";
        
        boolean orgFound = textWithBoundaries.contains(orgWithBoundaries);
        if (!orgFound) {
            log.warn("Organization '{}' NOT found in text for document: {}", organization, documentName);
            missingFields.add("company name");
        } else {
            log.info("Organization '{}' found in text", organization);
        }
        
        // Step 2: Check dates BEFORE normalization (to preserve date format)
        // Check fromDate (MANDATORY)
        if (!checkDateInText(text, fromDate, documentName)) {
            log.warn("From date '{}' NOT found in text for document: {}", fromDate, documentName);
            missingFields.add("from date");
        } else {
            log.info("From date '{}' found in text", fromDate);
        }
        
        // Conditional logic based on isPresentlyWorking
        if (isPresentlyWorking) {
            // If presently working: organization AND fromDate are sufficient
            // No need to check toDate
            log.info("Candidate is presently working - skipping toDate check");
        } else {
            // Not presently working: need organization AND (role OR postHeld) AND fromDate AND toDate
            
            // Step 3: Check role OR postHeld (at least one must be present)
            boolean roleFound = false;
            boolean postHeldFound = false;
            
            if (role != null && !role.trim().isEmpty()) {
                String normalizedRole = normalizeTextForMatching(role);
                String roleWithBoundaries = " " + normalizedRole + " ";
                roleFound = textWithBoundaries.contains(roleWithBoundaries);
                if (roleFound) {
                    log.info("Role '{}' found in text", role);
                }
            }
            
            if (postHeld != null && !postHeld.trim().isEmpty()) {
                String normalizedPostHeld = normalizeTextForMatching(postHeld);
                String postHeldWithBoundaries = " " + normalizedPostHeld + " ";
                postHeldFound = textWithBoundaries.contains(postHeldWithBoundaries);
                if (postHeldFound) {
                    log.info("Post held '{}' found in text", postHeld);
                }
            }
            
            if (!roleFound && !postHeldFound) {
                log.warn("Neither role '{}' nor post held '{}' found in text for document: {}", role, postHeld, documentName);
                missingFields.add("role or post held");
            }
            
            // Step 4: Check toDate (MANDATORY when not presently working)
            if (toDate == null || toDate.trim().isEmpty()) {
                log.error("To date is required when not presently working but was not provided");
                missingFields.add("to date");
            } else if (!checkDateInText(text, toDate, documentName)) {
                log.warn("To date '{}' NOT found in text for document: {}", toDate, documentName);
                missingFields.add("to date");
            } else {
                log.info("To date '{}' found in text", toDate);
            }
        }
        
        boolean fieldsFound = missingFields.isEmpty();
        if (fieldsFound) {
            log.info("Work experience validation successful - all required fields found");
        } else {
            log.warn("Work experience validation incomplete - missing fields: {}", missingFields);
        }
        
        return new WorkExpFieldValidationResult(fieldsFound, missingFields);
    }
    
    /**
     * Multi-stage OCR validation for work experience PDFs
     */
    private String validateWorkExpPdfWithMultiStageOcr(byte[] fileBytes, String extractedText, String organization, 
                                                         String role, String postHeld, String fromDate, String toDate,
                                                         boolean isPresentlyWorking, JsonNode keywordsNode, 
                                                         String documentName, String filename){
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes);
             PDDocument document = PDDocument.load(is)) {
            
            // Stage 1: Text extraction already attempted in calling method
            
            // Stage 2: Try to extract embedded images first
            List<BufferedImage> embeddedImages = extractEmbeddedImagesFromPdf(document);
            
            if (!embeddedImages.isEmpty()) {
                log.info("Found {} embedded image(s) in PDF, running OCR on embedded images...", embeddedImages.size());
                
                String embeddedOcrText = runOcrOnImages(embeddedImages);
                String combinedText = combineTexts(extractedText, embeddedOcrText);
                
                if (combinedText != null && !combinedText.trim().isEmpty()) {
                    String status = checkWorkExpFieldsAndKeywords(combinedText, organization, role, postHeld, fromDate, toDate, 
                            isPresentlyWorking, keywordsNode, documentName);
                    if (AppConstants.VALIDATION_STATUS_VALIDATED.equals(status) || AppConstants.VALIDATION_STATUS_PENDING.equals(status)) {
                        log.info("Validation completed with embedded images OCR for: {} - Status: {}", filename, status);
                        return status;
                    }
                    log.info("Status rejected in embedded images OCR. Will try page rendering.");
                }
            } else {
                log.info(AppConstants.NO_EMBEDDED_IMAGES_FOUND_IN_PDF);
            }
            
            // Stage 3: Render pages at 300 DPI as fallback
            log.info("Rendering PDF pages at 300 DPI for OCR...");
            List<BufferedImage> renderedImages = renderPdfPagesForIdProof(document);
            
            if (renderedImages.isEmpty()) {
                log.warn(AppConstants.PDF_NO_RENDERABLE_IMAGES);
                return AppConstants.VALIDATION_STATUS_REJECTED;
            }
            
            String renderedOcrText = runOcrOnImages(renderedImages);
            String finalCombinedText = combineTexts(extractedText, renderedOcrText);
            
            if (finalCombinedText != null && !finalCombinedText.trim().isEmpty()) {
                String status = checkWorkExpFieldsAndKeywords(finalCombinedText, organization, role, postHeld, 
                    fromDate, toDate, isPresentlyWorking, keywordsNode, documentName);
                log.info("Validation completed with page rendering OCR for: {} - Status: {}", filename, status);
                return status;
            }
            
            log.warn("No text extracted from PDF OCR for: {}", filename);
            return AppConstants.VALIDATION_STATUS_REJECTED;
            
        } catch (IOException e) {
            log.error("Error processing PDF with multi-stage OCR: {}", e.getMessage(), e);
            throw new CommonException("Error processing PDF");
        }
    }
    
    /**
     * Multi-stage OCR validation for work experience DOCX/DOC files
     */
    private String validateWorkExpDocxDocWithMultiStageOcr(byte[] fileBytes, String extractedText, String organization,
                                                             String role, String postHeld, String fromDate, String toDate,
                                                             boolean isPresentlyWorking, JsonNode keywordsNode, 
                                                             String documentName, String filename, String lowercasedFilename) throws IOException {
        // Extract embedded images from DOCX/DOC
        List<BufferedImage> embeddedImages;
        if (lowercasedFilename.endsWith(AppConstants.DOCX)) {
            embeddedImages = extractImagesFromDocx(fileBytes);
        } else {
            embeddedImages = extractImagesFromDoc(fileBytes);
        }
        
        if (embeddedImages.isEmpty()) {
            log.warn("No embedded images found in {} document: {}", lowercasedFilename.toUpperCase(), filename);
            return AppConstants.VALIDATION_STATUS_REJECTED;
        }
        
        log.info("Found {} embedded image(s) in {} document, running OCR...", embeddedImages.size(), lowercasedFilename.toUpperCase());
        
        String embeddedOcrText = runOcrOnImages(embeddedImages);
        String combinedText = combineTexts(extractedText, embeddedOcrText);
        
        if (combinedText != null && !combinedText.trim().isEmpty()) {
            String status = checkWorkExpFieldsAndKeywords(combinedText, organization, role, postHeld, fromDate, toDate, 
                isPresentlyWorking, keywordsNode, documentName);
            log.info("Validation completed with embedded images OCR for: {} - Status: {}", filename, status);
            return status;
        }
        
        log.warn("No text extracted from {} OCR for: {}", lowercasedFilename.toUpperCase(), filename);
        return AppConstants.VALIDATION_STATUS_REJECTED;
    }
    
    /**
     * Multi-stage OCR validation for birthdate proof PDFs
     */
    private String validateBirthdatePdfWithMultiStageOcr(byte[] fileBytes, String extractedText, String dateOfBirth, 
                                                           String fullNameAsPerCertificate, JsonNode keywordsNode, 
                                                           String documentName, String filename) {
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes);
             PDDocument document = PDDocument.load(is)) {
            
            // Stage 1: Text extraction already attempted in calling method
            // If we're here, either no text was extracted or date/keywords were not found in extracted text
            
            // Stage 2: Try to extract embedded images first
            List<BufferedImage> embeddedImages = extractEmbeddedImagesFromPdf(document);
            
            if (!embeddedImages.isEmpty()) {
                log.info("Found {} embedded image(s) in PDF, running OCR on embedded images...", embeddedImages.size());
                
                // Run OCR on embedded images
                String embeddedOcrText = runOcrOnImages(embeddedImages);
                
                // Combine with extracted text and validate
                String combinedText = combineTexts(extractedText, embeddedOcrText);
                
                if (combinedText != null && !combinedText.trim().isEmpty()) {
                    String status = checkDateOfBirthAndKeywords(combinedText, dateOfBirth, fullNameAsPerCertificate, keywordsNode, documentName);
                    if (AppConstants.VALIDATION_STATUS_VALIDATED.equals(status) || AppConstants.VALIDATION_STATUS_PENDING.equals(status)) {
                        log.info("Validation completed with embedded images OCR for: {} - Status: {}", filename, status);
                        return status;
                    }
                    log.info("Status rejected in embedded images OCR. Will try page rendering.");
                }
            } else {
                log.info(AppConstants.NO_EMBEDDED_IMAGES_FOUND_IN_PDF);
            }
            
            // Stage 3: Render pages at 300 DPI as fallback
            log.info("Rendering PDF pages at 300 DPI for OCR...");
            List<BufferedImage> renderedImages = renderPdfPagesForIdProof(document);
            
            if (renderedImages.isEmpty()) {
                log.warn(AppConstants.PDF_NO_RENDERABLE_IMAGES);
                return AppConstants.VALIDATION_STATUS_REJECTED;
            }
            
            // Run OCR on rendered pages
            String renderedOcrText = runOcrOnImages(renderedImages);
            
            // Combine with extracted text and validate
            String finalCombinedText = combineTexts(extractedText, renderedOcrText);
            
            if (finalCombinedText != null && !finalCombinedText.trim().isEmpty()) {
                String status = checkDateOfBirthAndKeywords(finalCombinedText, dateOfBirth, fullNameAsPerCertificate, keywordsNode, documentName);
                log.info("Validation completed with page rendering OCR for: {} - Status: {}", filename, status);
                return status;
            }
            
            log.warn("No text extracted from PDF OCR for: {}", filename);
            return AppConstants.VALIDATION_STATUS_REJECTED;
            
        } catch (IOException e) {
            log.error("Error processing PDF with multi-stage OCR: {}", e.getMessage(), e);
            throw new CommonException("Error processing PDF");
        }
    }
    
    /**
     * Multi-stage OCR validation for birthdate proof DOCX/DOC files
     */
    private String validateBirthdateDocxDocWithMultiStageOcr(byte[] fileBytes, String extractedText, String dateOfBirth,
                                                               String fullNameAsPerCertificate, JsonNode keywordsNode, 
                                                               String documentName, String filename, String lowercasedFilename) throws IOException {
        // Extract embedded images from DOCX/DOC
        List<BufferedImage> embeddedImages;
        if (lowercasedFilename.endsWith(AppConstants.DOCX)) {
            embeddedImages = extractImagesFromDocx(fileBytes);
        } else {
            embeddedImages = extractImagesFromDoc(fileBytes);
        }
        
        if (embeddedImages.isEmpty()) {
            log.warn("No embedded images found in {} document: {}", lowercasedFilename.toUpperCase(), filename);
            return AppConstants.VALIDATION_STATUS_REJECTED;
        }
        
        log.info("Found {} embedded image(s) in {} document, running OCR...", embeddedImages.size(), lowercasedFilename.toUpperCase());
        
        // Run OCR on embedded images
        String embeddedOcrText = runOcrOnImages(embeddedImages);
        
        // Combine with extracted text and validate
        String combinedText = combineTexts(extractedText, embeddedOcrText);
        
        if (combinedText != null && !combinedText.trim().isEmpty()) {
            String status = checkDateOfBirthAndKeywords(combinedText, dateOfBirth, fullNameAsPerCertificate, keywordsNode, documentName);
            log.info("Validation completed with embedded images OCR for: {} - Status: {}", filename, status);
            return status;
        }
        
        log.warn("No text extracted from {} OCR for: {}", lowercasedFilename.toUpperCase(), filename);
        return AppConstants.VALIDATION_STATUS_REJECTED;
    }

    // ===== WORK EXPERIENCE DOCUMENT VALIDATION =====
    
    public DocumentValidationResult validateWorkExpDocument(MultipartFile file, UUID documentId, String organization, String role, 
                                            String postHeld, String fromDate, String toDate, Boolean isPresentlyWorking) {
        try {
            // Fetch document type data by id
            DocumentTypesEntity documentType = documentTypesRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException(AppConstants.DOCUMENT_TYPE_NOT_FOUND_MESSAGE));

            // Check if keywords are configured
            JsonNode keywordsNode = documentType.getKeywords();
            
            String documentName = documentType.getDocumentName();
            
            log.info("Starting work experience validation for document type: {}", documentName);
            log.info("Organization: {}, Role: {}, Post Held: {}", organization, role, postHeld);
            log.info("From Date: {}, To Date: {}, Presently Working: {}", fromDate, toDate, isPresentlyWorking);
            
            // Conditional validation based on isPresentlyWorking
            if (isPresentlyWorking) {
                log.info("Candidate is presently working. Will validate: organization AND fromDate");
                // If no keywords configured, validate org + fromDate only
                if (keywordsNode == null || !keywordsNode.isArray() || keywordsNode.isEmpty()) {
                    log.info("No keywords configured. Will validate organization and fromDate only.");
                    WorkExpFieldValidationResult result = validateWorkExpFields(file, organization, null, null, fromDate, null, true, documentName);
                    String status = result.isFieldsFound() ? AppConstants.VALIDATION_STATUS_VALIDATED : AppConstants.VALIDATION_STATUS_REJECTED;
                    List<String> pendingChecks = result.isFieldsFound() ? null : result.getMissingFields();
                    return new DocumentValidationResult(status, pendingChecks);
                }
                // Validate org + fromDate + keywords
                String statusWithMessage = validateWorkExpFieldsAndKeywords(file, organization, null, null, fromDate, null, true, 
                    keywordsNode, documentName);
                return parseWorkExpStatusResult(statusWithMessage);
            } else {
                log.info("Candidate is not presently working. Will validate: organization AND (role OR postHeld) AND fromDate AND toDate");
                // If no keywords configured, validate org + (role OR postHeld) + fromDate + toDate
                if (keywordsNode == null || !keywordsNode.isArray() || keywordsNode.isEmpty()) {
                    log.info("No keywords configured. Will validate organization, role/postHeld, and dates only.");
                    WorkExpFieldValidationResult result = validateWorkExpFields(file, organization, role, postHeld, fromDate, toDate, false, documentName);
                    String status = result.isFieldsFound() ? AppConstants.VALIDATION_STATUS_VALIDATED : AppConstants.VALIDATION_STATUS_REJECTED;
                    List<String> pendingChecks = result.isFieldsFound() ? null : result.getMissingFields();
                    return new DocumentValidationResult(status, pendingChecks);
                }
                // Validate org + (role OR postHeld) + fromDate + toDate + keywords
                String statusWithMessage = validateWorkExpFieldsAndKeywords(file, organization, role, postHeld, fromDate, toDate, false, 
                    keywordsNode, documentName);
                return parseWorkExpStatusResult(statusWithMessage);
            }
            
        } catch (Exception e) {
            log.error("Error validating work experience document: {}", e.getMessage(), e);
            throw new CommonException("Error validating work experience document");
        }
    }
    
    /**
     * Parse status string like "validated", "pending:field1,field2", "rejected:document"
     * into DocumentValidationResult with status and pendingChecks array
     */
    private DocumentValidationResult parseWorkExpStatusResult(String statusWithMessage) {
        if (!statusWithMessage.contains(":")) {
            // Simple status like "validated"
            return new DocumentValidationResult(statusWithMessage, null);
        }
        
        String[] parts = statusWithMessage.split(":", 2);
        String status = parts[0];
        String message = parts[1];
        
        // Split comma-separated missing fields
        List<String> pendingChecks = Arrays.asList(message.split(","));
        
        return new DocumentValidationResult(status, pendingChecks);
    }
    
    private WorkExpFieldValidationResult validateWorkExpFields(MultipartFile file, String organization, String role, String postHeld,
                                           String fromDate, String toDate, boolean isPresentlyWorking, 
                                           String documentName) throws TesseractException, IOException {
        // Extract text from file
        String extractedText = extractAllTextFromFile(file, documentName);
        
        if (extractedText == null || extractedText.trim().isEmpty()) {
            log.warn("No text extracted from file for work experience validation");
            return new WorkExpFieldValidationResult(false, List.of(AppConstants.Document));
        }
        
        // Check for required fields
        WorkExpFieldValidationResult result = checkWorkExpFieldsInText(extractedText, organization, role, postHeld, fromDate, toDate, 
            isPresentlyWorking, documentName);
        return result;
    }
    
    private String validateWorkExpFieldsAndKeywords(MultipartFile file, String organization, String role, String postHeld,
                                                      String fromDate, String toDate, boolean isPresentlyWorking,
                                                      JsonNode keywordsNode, String documentName) throws TesseractException, IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        // Handle image files with OCR
        if (Arrays.asList(AppConstants.IMAGE_EXTENSIONS).contains(extension)) {
            log.debug("Processing work exp image file with OCR: {}", filename);
            String ocrText = ocrService.extractTextFromFile(file);
            
            if (ocrText == null || ocrText.trim().isEmpty()) {
                log.warn(AppConstants.OCR_TEXT_NOT_EXTRACTED_MESSAGE);
                return AppConstants.VALIDATION_STATUS_REJECTED;
            }
            
            return checkWorkExpFieldsAndKeywords(ocrText, organization, role, postHeld, fromDate, toDate, 
                isPresentlyWorking, keywordsNode, documentName);
        }
        
        // Handle document files (PDF, DOC, DOCX) with conditional OCR fallback
        else if (Arrays.asList(AppConstants.DOCUMENT_EXTENSIONS).contains(extension)) {
            log.debug("Processing work exp document file: {}", filename);
            return validateWorkExpDocumentFileWithFallback(file, organization, role, postHeld, fromDate, toDate, 
                isPresentlyWorking, keywordsNode, documentName);
        }
        
        else {
            throw new InvalidFileTypeException(
                "Unsupported file format: " + extension.toUpperCase() + 
                ". Supported formats: JPG, PNG, TIFF, BMP, GIF, PDF, DOC, DOCX"
            );
        }
    }
    
    private String validateWorkExpDocumentFileWithFallback(MultipartFile file, String organization, String role, String postHeld,
                                                             String fromDate, String toDate, boolean isPresentlyWorking,
                                                             JsonNode keywordsNode, String documentName) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }
        String lowercasedFilename = filename.toLowerCase();
        
        Path tempFilePath = null;
        try {
            String extension = null;
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = filename.substring(dotIndex);
            }
            tempFilePath = TempDirUtil.createSecureTempFile("workexp_validation_", extension);
            file.transferTo(tempFilePath);
            byte[] fileBytes = Files.readAllBytes(tempFilePath);
            
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
                    return AppConstants.VALIDATION_STATUS_REJECTED;
                }
            }
            
            // Stage 1: Check if extracted text has all required fields AND keywords
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                String status = checkWorkExpFieldsAndKeywords(extractedText, organization, role, postHeld, fromDate, toDate, 
                        isPresentlyWorking, keywordsNode, documentName);
                if (AppConstants.VALIDATION_STATUS_VALIDATED.equals(status) || AppConstants.VALIDATION_STATUS_PENDING.equals(status)) {
                    log.info("Validation completed with text extraction for: {} - Status: {}", filename, status);
                    return status;
                }
                log.info("Status rejected in extracted text ({} chars). Will try OCR fallback.", 
                    extractedText.length());
            } else {
                log.info(AppConstants.OCR_TEXT_NOT_EXTRACTED_FALLBACK_MESSAGE);
            }
            
            // Stage 2 and Stage 3: Multi-stage OCR approach for all document types
            if (lowercasedFilename.endsWith(".pdf")) {
                return validateWorkExpPdfWithMultiStageOcr(fileBytes, extractedText, organization, role, postHeld, 
                    fromDate, toDate, isPresentlyWorking, keywordsNode, documentName, filename);
            } else if (lowercasedFilename.endsWith(AppConstants.DOCX) || lowercasedFilename.endsWith(".doc")) {
                return validateWorkExpDocxDocWithMultiStageOcr(fileBytes, extractedText, organization, role, postHeld, 
                    fromDate, toDate, isPresentlyWorking, keywordsNode, documentName, filename, lowercasedFilename);
            }
            
            log.warn("Unsupported document type for OCR: {}", filename);
            return AppConstants.VALIDATION_STATUS_REJECTED;
            
        } finally {
            TempDirUtil.deleteIfExists(tempFilePath);
        }
    }

    private String extractAllTextFromFile(MultipartFile file, String documentName) throws TesseractException, IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        // Handle image files with OCR
        if (Arrays.asList(AppConstants.IMAGE_EXTENSIONS).contains(extension)) {
            log.debug("Processing image file with OCR: {}", filename);
            return ocrService.extractTextFromFile(file);
        }
        
        // Handle document files (PDF, DOC, DOCX)
        else if (Arrays.asList(AppConstants.DOCUMENT_EXTENSIONS).contains(extension)) {
            log.debug("Processing document file: {}", filename);
            return extractAllTextFromDocumentFile(file, documentName);
        }
        
        else {
            throw new InvalidFileTypeException(
                "Unsupported file format: " + extension.toUpperCase() + 
                ". Supported formats: JPG, PNG, TIFF, BMP, GIF, PDF, DOC, DOCX"
            );
        }
    }

    private String extractAllTextFromDocumentFile(MultipartFile file, String documentName) throws IOException {
        // This method is NOT used for ID proof validation
        // ID proof uses validateDocumentNumberAndKeywords which has conditional OCR fallback
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }
        String lowercasedFilename = filename.toLowerCase();
        
        Path tempFilePath = null;
        try {
            String extension = null;
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = filename.substring(dotIndex);
            }
            tempFilePath = TempDirUtil.createSecureTempFile("idproof_validation_", extension);
            file.transferTo(tempFilePath);
            byte[] fileBytes = Files.readAllBytes(tempFilePath);
            
            // Try text extraction
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
                    return null;
                }
            }
            
            // Try OCR fallback
            log.info("Attempting OCR on embedded images for: {}", filename);
            String ocrText = extractTextFromDocumentImagesWithOcr(fileBytes, lowercasedFilename);
            
            // Combine both texts
            String combinedText;
            if (extractedText != null && !extractedText.trim().isEmpty() && ocrText != null) {
                combinedText = extractedText + "\n" + ocrText;
            } else if (ocrText != null) {
                combinedText = ocrText;
            } else {
                combinedText = extractedText;
            }
            
            log.info("Text extraction completed for ID proof. Total characters: {}", 
                combinedText != null ? combinedText.length() : 0);
            
            return combinedText;
            
        } finally {
            TempDirUtil.deleteIfExists(tempFilePath);
        }
    }

    // ===== BIRTHDATE PROOF VALIDATION =====
    
    public DocumentValidationResult validateBirthdateProofDocument(MultipartFile file, UUID documentId, String fullNameAsPerCertificate, String dateOfBirth) {
        try {
            // Fetch document type data by id
            DocumentTypesEntity documentType = documentTypesRepository.findById(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException(AppConstants.DOCUMENT_TYPE_NOT_FOUND_MESSAGE));

            // Check if keywords are configured
            JsonNode keywordsNode = documentType.getKeywords();
            
            String documentName = documentType.getDocumentName();
            
            log.info("Starting birthdate proof validation for document type: {}", documentName);
            log.info("Full name as per certificate: {} (Note: Name validation not implemented yet due to spelling ambiguity)", 
                fullNameAsPerCertificate);
            log.info("Date to validate: {}", dateOfBirth);
            
            // If no keywords configured (null or empty), only validate date
            if (keywordsNode == null || !keywordsNode.isArray() || keywordsNode.isEmpty()) {
                log.info("No keywords configured for document type: {}. Will only validate date.", documentName);
                boolean dateFound = validateDateOfBirthInFile(file, dateOfBirth, documentName);
                String status = dateFound ? AppConstants.VALIDATION_STATUS_VALIDATED : AppConstants.VALIDATION_STATUS_REJECTED;
                List<String> pendingChecks = dateFound ? null : List.of("date of birth");
                return new DocumentValidationResult(status, pendingChecks);
            }

            // Validate both date AND keywords
            String status = validateDateOfBirthAndKeywords(file, dateOfBirth, fullNameAsPerCertificate, keywordsNode, documentName);
            
            // Map status to pendingChecks
            List<String> pendingChecks;
            if (AppConstants.VALIDATION_STATUS_VALIDATED.equals(status)) {
                pendingChecks = null;
            } else if (AppConstants.VALIDATION_STATUS_PENDING.equals(status)) {
                pendingChecks = List.of("date of birth");
            } else {
                pendingChecks = List.of(AppConstants.Document);
            }
            
            return new DocumentValidationResult(status, pendingChecks);
            
        } catch (Exception e) {
            log.error("Error validating birthdate proof document: {}", e.getMessage(), e);
            throw new CommonException("Error validating birthdate proof document");
        }
    }
    
    private boolean validateDateOfBirthInFile(MultipartFile file, String dateOfBirth, String documentName) throws TesseractException, IOException {
        // Extract text from file
        String extractedText = extractAllTextFromFile(file, documentName);
        
        if (extractedText == null || extractedText.trim().isEmpty()) {
            log.warn("No text extracted from file for date validation");
            return false;
        }
        
        // Check for date with flexible format matching
        boolean found = checkDateInText(extractedText, dateOfBirth, documentName);
        if (found) {
            log.info("Date '{}' found in document: {}", dateOfBirth, documentName);
        } else {
            log.warn("Date '{}' NOT found in document: {}", dateOfBirth, documentName);
        }
        return found;
    }
    
    private String validateDateOfBirthAndKeywords(MultipartFile file, String dateOfBirth, String fullNameAsPerCertificate,
                                                     JsonNode keywordsNode, String documentName) throws TesseractException, IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.contains(".")) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        // Handle image files with OCR
        if (Arrays.asList(AppConstants.IMAGE_EXTENSIONS).contains(extension)) {
            log.debug("Processing birthdate proof image file with OCR: {}", filename);
            String ocrText = ocrService.extractTextFromFile(file);
            
            if (ocrText == null || ocrText.trim().isEmpty()) {
                log.warn(AppConstants.OCR_TEXT_NOT_EXTRACTED_MESSAGE);
                return AppConstants.VALIDATION_STATUS_REJECTED;
            }
            
            return checkDateOfBirthAndKeywords(ocrText, dateOfBirth, fullNameAsPerCertificate, keywordsNode, documentName);
        }
        
        // Handle document files (PDF, DOC, DOCX) with conditional OCR fallback
        else if (Arrays.asList(AppConstants.DOCUMENT_EXTENSIONS).contains(extension)) {
            log.debug("Processing birthdate proof document file: {}", filename);
            return validateBirthdateDocumentFileWithFallback(file, dateOfBirth, fullNameAsPerCertificate, keywordsNode, documentName);
        }
        
        else {
            throw new InvalidFileTypeException(
                "Unsupported file format: " + extension.toUpperCase() + 
                ". Supported formats: JPG, PNG, TIFF, BMP, GIF, PDF, DOC, DOCX"
            );
        }
    }
    
    private String validateBirthdateDocumentFileWithFallback(MultipartFile file, String dateOfBirth, String fullNameAsPerCertificate,
                                                               JsonNode keywordsNode, String documentName) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }
        String lowercasedFilename = filename.toLowerCase();
        
        Path tempFilePath = null;
        try {
            String extension = null;
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = filename.substring(dotIndex);
            }

            tempFilePath = TempDirUtil.createSecureTempFile("birthdate_validation_", extension);
            
            file.transferTo(tempFilePath);
            byte[] fileBytes = Files.readAllBytes(tempFilePath);
            
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
                    return AppConstants.VALIDATION_STATUS_REJECTED;
                }
            }catch (IOException e){
                throw new CommonException("Failed to extract text");
            }
            
            // Stage 1: Check if extracted text has both date AND keywords
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                String status = checkDateOfBirthAndKeywords(extractedText, dateOfBirth, fullNameAsPerCertificate, keywordsNode, documentName);
                if (AppConstants.VALIDATION_STATUS_VALIDATED.equals(status) || AppConstants.VALIDATION_STATUS_PENDING.equals(status)) {
                    log.info("Validation completed with text extraction for: {} - Status: {}", filename, status);
                    return status;
                }
                log.info("Status rejected in extracted text ({} chars). Will try OCR fallback.", 
                    extractedText.length());
            } else {
                log.info(AppConstants.OCR_TEXT_NOT_EXTRACTED_FALLBACK_MESSAGE);
            }
            
            // Stage 2 and Stage 3: Multi-stage OCR approach for all document types
            if (lowercasedFilename.endsWith(".pdf")) {
                return validateBirthdatePdfWithMultiStageOcr(fileBytes, extractedText, dateOfBirth, fullNameAsPerCertificate, keywordsNode, documentName, filename);
            } else if (lowercasedFilename.endsWith(AppConstants.DOCX) || lowercasedFilename.endsWith(".doc")) {
                return validateBirthdateDocxDocWithMultiStageOcr(fileBytes, extractedText, dateOfBirth, fullNameAsPerCertificate, keywordsNode, documentName, filename, lowercasedFilename);
            }
            
            log.warn("Unsupported document type for OCR: {}", filename);
            return AppConstants.VALIDATION_STATUS_REJECTED;
            
        } finally {
           TempDirUtil.deleteIfExists(tempFilePath);
        }
    }

    public boolean validateDocument(MultipartFile file, String docCode) {
        try {
            // Fetch document type data by doc_code
            DocumentTypesEntity documentType = documentTypesRepository.findByDocCode(docCode);
            if (documentType == null) {
                throw new ResourceNotFoundException("Document type not found for code: " + docCode);
            }

            // Check if keywords are configured
            JsonNode keywordsNode = documentType.getKeywords();
            
            // If no keywords configured (null or empty), pass validation
            if (keywordsNode == null || !keywordsNode.isArray() || keywordsNode.isEmpty()) {
                log.info("No keywords configured for document type: {}. Validation passed by default.", 
                    documentType.getDocumentName());
                return true;
            }

            // Determine file type and validate accordingly
            return extractTextFromFile(file, keywordsNode, documentType.getDocumentName());

        } catch (Exception e) {
            log.error("Error validating document: {}", e.getMessage(), e);
            throw new CommonException("Error validating document");
        }
    }

    private boolean extractTextFromFile(MultipartFile file, JsonNode keywordsNode, String documentName) throws TesseractException, IOException {
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
                boolean isValid = validateKeywords(ocrText, keywordsNode, documentName);
                if (isValid) {
                    log.info("Image validation successful - keywords found in OCR text");
                } else {
                    log.warn("Image validation failed - keywords not found in OCR text");
                }
                return isValid;
            } else {
                log.warn(AppConstants.OCR_TEXT_NOT_EXTRACTED_MESSAGE);
                return false;
            }
        }
        
        // Handle document files (PDF, DOC, DOCX)
        else if (Arrays.asList(AppConstants.DOCUMENT_EXTENSIONS).contains(extension)) {
            log.debug("Processing document file: {}", filename);
            return extractTextFromDocumentFile(file, keywordsNode, documentName);
        }
        
        else {
            throw new InvalidFileTypeException(
                "Unsupported file format: " + extension.toUpperCase() + 
                ". Supported formats: JPG, JPEG, PNG, PDF, DOC, DOCX"
            );
        }
    }

    private boolean extractTextFromDocumentFile(MultipartFile file, JsonNode keywordsNode, String documentName) throws IOException{
        // Get filename before transferring
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }
        String lowercasedFilename = filename.toLowerCase();
        
        // Create our own temp file and transfer MultipartFile to it
        Path tempFilePath = null;
        try {
            // Create temp file with appropriate extension
            String extension = null;
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = filename.substring(dotIndex);
            }
            tempFilePath = TempDirUtil.createSecureTempFile("doc_validation_", extension);
            
            // Transfer uploaded file to our temp file (releases original file)
            file.transferTo(tempFilePath);
            
            // Now read our temp file into byte array for processing
            byte[] fileBytes = Files.readAllBytes(tempFilePath);
            
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
            }catch (IOException e){
                throw new CommonException("Failed to extract text.");
            }
            
            // Stage 1: Check if extracted text has keywords
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                if (validateKeywords(extractedText, keywordsNode, documentName)) {
                    log.info("Validation successful with text extraction for: {}", filename);
                    return true;
                }
                log.info("Keywords not found in extracted text ({} chars). Will try OCR fallback.", 
                    extractedText.length());
            } else {
                log.info(AppConstants.OCR_TEXT_NOT_EXTRACTED_FALLBACK_MESSAGE);
            }
            
            // Stage 2 and Stage 3: Multi-stage OCR approach for all document types
            if (lowercasedFilename.endsWith(".pdf")) {
                return validatePdfWithMultiStageOcrKeywordsOnly(fileBytes, extractedText, keywordsNode, documentName, filename);
            } else if (lowercasedFilename.endsWith(AppConstants.DOCX) || lowercasedFilename.endsWith(".doc")) {
                return validateDocxDocWithMultiStageOcrKeywordsOnly(fileBytes, extractedText, keywordsNode, documentName, filename, lowercasedFilename);
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
    
    private String extractTextFromDocumentImagesWithOcr(byte[] fileBytes, String lowercasedFilename) throws IOException {
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
            
            // Render each page as an image at 300 DPI for better OCR
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
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
    
    private String runOcrOnImage(BufferedImage image, int imageIndex) {
        Path tempFilePath = null;
        try {
            // Create temporary file for OCR processing

            tempFilePath = TempDirUtil.createSecureTempFile("ocr_image_" + imageIndex + "_", ".png");
            
            // Convert to grayscale for consistent OCR
            BufferedImage grayscaleImage = new BufferedImage(
                image.getWidth(), 
                image.getHeight(), 
                BufferedImage.TYPE_BYTE_GRAY
            );
            java.awt.Graphics2D g = grayscaleImage.createGraphics();
            g.drawImage(image, 0, 0, null);
            g.dispose();
            
            // Use explicit PNG writer
            javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();
            javax.imageio.ImageWriteParam writeParam = writer.getDefaultWriteParam();
            javax.imageio.stream.ImageOutputStream ios = ImageIO.createImageOutputStream(tempFilePath.toFile());
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(grayscaleImage, null, null), writeParam);
            ios.close();
            writer.dispose();
            
            // Load tessdata and run OCR
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

    private boolean validateKeywords(String text, JsonNode keywordsNode, String documentName) {
        // Normalize extracted text for robust cross-platform matching
        String normalizedText = normalizeTextForMatching(text);
        
        log.debug("Validating document type: {}", documentName);
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
                log.info("Keyword matched: '{}' for document type: {}", keyword, documentName);
                return true;
            }
        }
        
        log.warn("No keywords matched for document type: {}", documentName);
        return false;
    }
    
    private String normalizeTextForMatching(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        return text
            .toLowerCase()
            .replaceAll("\\p{M}", "")
            .replaceAll("\\s+", " ")
            .replaceAll("[^a-z0-9\\s]", "")
            .trim();
    }
    
    // ===== ID Proof Validation Specific OCR Methods =====
    // These methods use 300 DPI and extract embedded images directly from PDFs
    
    /**
     * Extract embedded images from PDF document
     */
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
    
    /**
     * Render PDF pages as images at 300 DPI with whitespace cropping
     */
    private List<BufferedImage> renderPdfPagesForIdProof(PDDocument document) throws IOException {
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
    
    /**
     * Run OCR on a list of images and combine results
     */
    private String runOcrOnImages(List<BufferedImage> images) {
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
    
    private String extractTextFromIdProofImagesWithOcr(byte[] fileBytes, String lowercasedFilename) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        
        // Extract images based on document type
        if (lowercasedFilename.endsWith(".pdf")) {
            images = extractImagesFromPdfForIdProof(fileBytes);
        } else if (lowercasedFilename.endsWith(AppConstants.DOCX)) {
            images = extractImagesFromDocx(fileBytes);
        } else if (lowercasedFilename.endsWith(".doc")) {
            images = extractImagesFromDoc(fileBytes);
        }
        
        if (images.isEmpty()) {
            log.warn("No images found in ID proof document");
            return null;
        }
        
        log.info("Found {} image(s) in ID proof document. Running OCR...", images.size());
        
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
        log.info("OCR completed for ID proof. Extracted {} characters from images", result.length());
        return result.isEmpty() ? null : result;
    }
    
    private List<BufferedImage> extractImagesFromPdfForIdProof(byte[] fileBytes) throws IOException {
        List<BufferedImage> images = new ArrayList<>();
        
        try (InputStream is = new java.io.ByteArrayInputStream(fileBytes);
             PDDocument document = PDDocument.load(is)) {
            
            // First, try to extract embedded images directly (original scans/photos)
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
                                log.info("Extracted embedded image from ID proof PDF page {} ({}x{}) - using original scan", 
                                    pageIndex, bImage.getWidth(), bImage.getHeight());
                                images.add(bImage);
                            }
                        }
                    }
                }
            }
            
            // If no embedded images found, fall back to page rendering at 300 DPI
            if (images.isEmpty()) {
                log.info("No embedded images found in ID proof PDF, falling back to page rendering at 300 DPI");
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                
                for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                    BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, 300);
                    
                    // Crop excessive whitespace from rendered pages
                    BufferedImage croppedImage = cropWhitespace(pageImage);
                    
                    log.info("Rendered ID proof page {} at 300 DPI ({}x{} -> {}x{} after cropping)", 
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
            }
        }
        
        return images;
    }
    
    private BufferedImage cropWhitespace(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Define threshold for "white" pixels (allowing slight variations)
        int whiteThreshold = 240;
        
        // Find bounds by scanning from each edge
        int top = 0;
        int bottom = height - 1;
        int left = 0;
        int right = width - 1;
        
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
        
        // Add small padding (5% of remaining dimensions) to avoid cutting too close
        int padding = Math.max(10, Math.min((right - left) / 20, (bottom - top) / 20));
        top = Math.max(0, top - padding);
        bottom = Math.min(height - 1, bottom + padding);
        left = Math.max(0, left - padding);
        right = Math.min(width - 1, right + padding);
        
        // Calculate new dimensions
        int newWidth = right - left + 1;
        int newHeight = bottom - top + 1;
        
        // If crop would be too small or invalid, return original
        if (newWidth <= 0 || newHeight <= 0 || newWidth < width / 4 || newHeight < height / 4) {
            log.warn("Crop dimensions invalid or too small ({}x{}), using original image", newWidth, newHeight);
            return image;
        }
        
        log.debug("Cropping image from {}x{} to {}x{} (removed {}% whitespace)", 
            width, height, newWidth, newHeight, 
            100 - (newWidth * newHeight * 100 / (width * height)));
        
        // Crop the image
        return image.getSubimage(left, top, newWidth, newHeight);
    }
}
