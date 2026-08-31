package com.bob.commonutil.util;

import com.bob.commonutil.exception.InvalidFileTypeException;
import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Centralized file validation for uploads.
 * Uses Apache Tika to detect actual file content (not client-supplied MIME or filename).
 * Allowed types are configured in {@link AppConstants#ALL_ALLOWED_EXTENSIONS}.
 */
public class FileValidationUtil {

    private static final Tika TIKA = new Tika();

    private static final Map<String, String> EXTENSION_TO_MIME = new HashMap<>();
    private static final Map<String, String> MIME_TO_EXTENSION = new HashMap<>();

    /** Extensions that must never appear in an uploaded filename (incl. double-extension tricks). */
    private static final Set<String> BLACKLISTED_EXTENSIONS = Set.of(
            "exe", "bat", "cmd", "com", "msi", "dll", "scr", "ps1", "vbs", "js", "jar", "war",
             "rar", "7z", "tar", "gz", "bz2", "html", "htm", "svg", "php", "asp", "aspx",
            "jsp", "sh", "bash", "bin", "apk", "deb", "rpm", "iso", "dmg", "csv"
    );

    /** MIME types detected from content that are always rejected. */
    private static final Set<String> BLACKLISTED_MIME_TYPES = Set.of(
            "application/x-msdownload",
            "application/x-dosexec",
            "application/x-executable",
            "application/java-archive",
            "application/x-rar-compressed",
            "application/gzip",
            "application/x-7z-compressed",
            "text/html",
            "application/xhtml+xml",
            "image/svg+xml",
            "application/javascript",
            "text/javascript",
            "text/plain"
    );

    static {
        EXTENSION_TO_MIME.put(AppConstants.FILE_EXTENSION_JPG, AppConstants.MIME_IMAGE_JPG);
        EXTENSION_TO_MIME.put(AppConstants.FILE_EXTENSION_JPEG, AppConstants.MIME_IMAGE_JPEG);
        EXTENSION_TO_MIME.put(AppConstants.FILE_EXTENSION_PNG, AppConstants.MIME_IMAGE_PNG);
        EXTENSION_TO_MIME.put(AppConstants.FILE_EXTENSION_DOCX, AppConstants.MIME_DOCX);
        EXTENSION_TO_MIME.put(AppConstants.FILE_EXTENSION_DOC, AppConstants.MIME_DOC);
        EXTENSION_TO_MIME.put(AppConstants.FILE_EXTENSION_XLSX, AppConstants.MIME_XLSX);
        EXTENSION_TO_MIME.put(AppConstants.FILE_EXTENSION_XLS, AppConstants.MIME_XLS);
        EXTENSION_TO_MIME.put(AppConstants.FILE_EXTENSION_PDF, AppConstants.MIME_PDF);
        EXTENSION_TO_MIME.put(AppConstants.FILE_EXTENSION_ZIP,AppConstants.MIME_ZIP);
        for (Map.Entry<String, String> entry : EXTENSION_TO_MIME.entrySet()) {
            MIME_TO_EXTENSION.put(entry.getValue(), entry.getKey());
        }
    }

    /**
     * Returns true only for actual file upload parts (not JSON metadata parts in multipart forms).
     * Upload endpoints often send {@code file} + {@code request} (JSON) in one multipart request.
     */
    public static boolean shouldValidateMultipartPart(MultipartFile file, String paramName) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        if (file.getOriginalFilename() == null) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType != null) {
            String normalized = normalizeMimeType(contentType);
            if ("application/json".equals(normalized)) {
                return false;
            }
        }

        if (looksLikeJsonMetadataPart(file)) {
            return false;
        }

        return true;
    }

    /**
     * Validates an uploaded file using content inspection (Tika) plus filename safety checks.
     * Does not trust client-supplied Content-Type or filename extension alone.
     */
    public static void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            validateFilenameSafety(originalFilename);
        }

        final byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new InvalidFileTypeException("Unable to read uploaded file: " + e.getMessage());
        }

        if (content.length < 4) {
            throw new InvalidFileTypeException("File is too small or corrupted");
        }

        rejectExecutableMagicBytes(content);
        String detectedMime = detectContentMimeType(content, originalFilename);
        detectedMime = resolveAmbiguousTikaMime(detectedMime, content, originalFilename);
        validateContainerMatchesMime(content, detectedMime);

        if (detectedMime == null || detectedMime.isBlank()) {
            throw new InvalidFileTypeException("Unable to determine file type from content");
        }

        if (BLACKLISTED_MIME_TYPES.contains(detectedMime)) {
            throw new InvalidFileTypeException("File type not allowed: " + detectedMime);
        }

        String detectedExtension = MIME_TO_EXTENSION.get(detectedMime);
        if (detectedExtension == null || !isExtensionAllowed(detectedExtension)) {
            throw new InvalidFileTypeException(
                    "File type not allowed. Only " + String.join(", ", AppConstants.ALL_ALLOWED_EXTENSIONS)
                            + " files are accepted. Detected content type: " + detectedMime
            );
        }

        if (originalFilename != null && !isGenericOrMissingExtension(originalFilename)) {
            String filenameExtension = getFileExtension(originalFilename).toLowerCase();
            if (!extensionsMatch(filenameExtension, detectedExtension)) {
                throw new InvalidFileTypeException(
                        "File content does not match filename extension. Expected content for ."
                                + filenameExtension + " but detected: " + detectedMime
                );
            }
        }
    }

    public static String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null || mimeType.trim().isEmpty()) {
            return "";
        }
        return MIME_TO_EXTENSION.getOrDefault(normalizeMimeType(mimeType), "");
    }

    private static void validateFilenameSafety(String originalFilename) {
        if (originalFilename.trim().isEmpty()) {
            throw new InvalidFileTypeException("Filename cannot be empty");
        }
        if (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\")) {
            throw new InvalidFileTypeException("Filename contains invalid characters (path traversal attempt detected)");
        }

        String lower = originalFilename.toLowerCase(Locale.ROOT);
        for (String segment : lower.split("\\.")) {
            if (BLACKLISTED_EXTENSIONS.contains(segment)) {
                throw new InvalidFileTypeException("File type not allowed. Dangerous extension in filename: " + segment);
            }
        }
        rejectMultipleExtensions(originalFilename, lower);
    }

    /** Only {@code basename.ext} is allowed — no double/multiple extensions (e.g. payslip.txt.jpg). */
    private static void rejectMultipleExtensions(String originalFilename, String lowerFilename) {
        String[] segments = lowerFilename.split("\\.");
        if (segments.length > 2) {
            throw new InvalidFileTypeException(
                    "Multiple file extensions and additional dots are not allowed in the filename: " + originalFilename);
        }
        for (String segment : segments) {
            if (segment.isEmpty()) {
                throw new InvalidFileTypeException("Filename contains invalid characters: " + originalFilename);
            }
        }
    }

    private static void rejectExecutableMagicBytes(byte[] content) {
        if (content[0] == 'M' && content[1] == 'Z') {
            throw new InvalidFileTypeException("Executable files are not allowed");
        }
    }

    private static boolean isGenericOrMissingExtension(String filename) {
        String trimmed = filename.trim().toLowerCase(Locale.ROOT);
        return trimmed.equals("blob")
                || trimmed.equals("untitled")
                || trimmed.equals("file")
                || getFileExtension(filename).isEmpty();
    }

    private static boolean isExtensionAllowed(String extension) {
        for (String allowed : AppConstants.ALL_ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    private static boolean extensionsMatch(String filenameExtension, String detectedExtension) {
        if (filenameExtension.equalsIgnoreCase(detectedExtension)) {
            return true;
        }
        boolean filenameIsJpeg = filenameExtension.equals(AppConstants.FILE_EXTENSION_JPG)
                || filenameExtension.equals(AppConstants.FILE_EXTENSION_JPEG);
        boolean detectedIsJpeg = detectedExtension.equals(AppConstants.FILE_EXTENSION_JPG)
                || detectedExtension.equals(AppConstants.FILE_EXTENSION_JPEG);
        return filenameIsJpeg && detectedIsJpeg;
    }

    private static boolean looksLikeJsonMetadataPart(MultipartFile file) {
        try {
            byte[] content = file.getBytes();
            if (content.length == 0) {
                return false;
            }
            char first = (char) content[0];
            return first == '{' || first == '[';
        } catch (IOException e) {
            return false;
        }
    }

    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    private static String detectContentMimeType(byte[] content, String originalFilename) {
        if (originalFilename != null && !isGenericOrMissingExtension(originalFilename)) {
            return normalizeMimeType(TIKA.detect(content, originalFilename));
        }
        return normalizeMimeType(TIKA.detect(content));
    }

    /**
     * Tika often returns generic types for Microsoft Office files; resolve to concrete allowed MIME.
     */
    private static String resolveAmbiguousTikaMime(String detectedMime, byte[] content, String originalFilename) {
        if ("application/x-tika-ooxml".equals(detectedMime)) {
            if (!isZipContainer(content)) {
                throw new InvalidFileTypeException("File content is not a valid Office Open XML document");
            }
            String ext = originalFilename != null ? getFileExtension(originalFilename).toLowerCase(Locale.ROOT) : "";
            if (AppConstants.FILE_EXTENSION_XLSX.equals(ext)) {
                return AppConstants.MIME_XLSX;
            }
            if (AppConstants.FILE_EXTENSION_DOCX.equals(ext)) {
                return AppConstants.MIME_DOCX;
            }
            if (containsAsciiMarker(content, "xl/")) {
                return AppConstants.MIME_XLSX;
            }
            if (containsAsciiMarker(content, "word/")) {
                return AppConstants.MIME_DOCX;
            }
            throw new InvalidFileTypeException("Unrecognized Office document type");
        }
        if ("application/x-tika-msoffice".equals(detectedMime)) {
            String ext = originalFilename != null ? getFileExtension(originalFilename).toLowerCase(Locale.ROOT) : "";
            if (AppConstants.FILE_EXTENSION_XLS.equals(ext)) {
                return AppConstants.MIME_XLS;
            }
            if (AppConstants.FILE_EXTENSION_DOC.equals(ext)) {
                return AppConstants.MIME_DOC;
            }
            throw new InvalidFileTypeException("Unrecognized legacy Office document type");
        }
        return detectedMime;
    }

    private static void validateContainerMatchesMime(byte[] content, String mime) {
        if (AppConstants.MIME_XLSX.equals(mime) || AppConstants.MIME_DOCX.equals(mime)) {
            if (!isZipContainer(content)) {
                throw new InvalidFileTypeException(
                        "File content does not match Office Open XML format for: " + mime
                );
            }
        } else if (AppConstants.MIME_XLS.equals(mime) || AppConstants.MIME_DOC.equals(mime)) {
            if (!isOleContainer(content)) {
                throw new InvalidFileTypeException(
                        "File content does not match legacy Office format for: " + mime
                );
            }
        }
    }

    /** OOXML (.xlsx, .docx) files are ZIP containers and start with PK. */
    private static boolean isZipContainer(byte[] content) {
        return content.length >= 4
                && content[0] == 0x50
                && content[1] == 0x4B;
    }

    /** Legacy .xls / .doc use OLE compound document format. */
    private static boolean isOleContainer(byte[] content) {
        return content.length >= 8
                && content[0] == (byte) 0xD0
                && content[1] == (byte) 0xCF
                && content[2] == 0x11
                && content[3] == (byte) 0xE0
                && content[4] == (byte) 0xA1
                && content[5] == (byte) 0xB1
                && content[6] == 0x1A
                && content[7] == (byte) 0xE1;
    }

    private static boolean containsAsciiMarker(byte[] content, String marker) {
        byte[] markerBytes = marker.getBytes(StandardCharsets.US_ASCII);
        int searchLimit = Math.min(content.length - markerBytes.length, 8192);
        outer:
        for (int i = 0; i <= searchLimit; i++) {
            for (int j = 0; j < markerBytes.length; j++) {
                if (content[i + j] != markerBytes[j]) {
                    continue outer;
                }
            }
            return true;
        }
        return false;
    }

    private static String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return "";
        }
        String normalized = mimeType.toLowerCase(Locale.ROOT).trim();
        int semicolon = normalized.indexOf(';');
        if (semicolon > 0) {
            normalized = normalized.substring(0, semicolon).trim();
        }
        if ("image/jpg".equals(normalized)) {
            return AppConstants.MIME_IMAGE_JPEG;
        }
        return normalized;
    }
}
