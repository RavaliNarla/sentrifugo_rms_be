package com.bob.commonutil.util;

import org.springframework.http.MediaType;

import java.util.Locale;
import java.util.Map;

/**
 * Resolves accurate MIME types from filenames for download responses and blob storage headers.
 */
public final class FileContentTypeUtil {

    private static final Map<String, String> EXTENSION_TO_MIME = Map.of(
            AppConstants.FILE_EXTENSION_JPG, AppConstants.MIME_IMAGE_JPG,
            AppConstants.FILE_EXTENSION_JPEG, AppConstants.MIME_IMAGE_JPEG,
            AppConstants.FILE_EXTENSION_PNG, AppConstants.MIME_IMAGE_PNG,
            AppConstants.FILE_EXTENSION_PDF, AppConstants.MIME_PDF,
            AppConstants.FILE_EXTENSION_DOC, AppConstants.MIME_DOC,
            AppConstants.FILE_EXTENSION_DOCX, AppConstants.MIME_DOCX,
            AppConstants.FILE_EXTENSION_XLS, AppConstants.MIME_XLS,
            AppConstants.FILE_EXTENSION_XLSX, AppConstants.MIME_XLSX
    );

    private FileContentTypeUtil() {
    }

    public static String getMimeTypeFromFilename(String filename) {
        String extension = getExtension(filename);
        if (extension.isEmpty()) {
            return null;
        }
        return EXTENSION_TO_MIME.get(extension);
    }

    public static MediaType getMediaTypeFromFilename(String filename) {
        String mime = getMimeTypeFromFilename(filename);
        if (mime == null) {
            return null;
        }
        return MediaType.parseMediaType(mime);
    }

    public static MediaType resolveMediaType(String filename, MediaType current) {
        if (current != null && !MediaType.APPLICATION_OCTET_STREAM.equals(current)) {
            return current;
        }
        MediaType fromName = getMediaTypeFromFilename(filename);
        return fromName != null ? fromName : current;
    }

    public static String extractFilename(String pathOrName) {
        if (pathOrName == null || pathOrName.isBlank()) {
            return "download";
        }
        String normalized = pathOrName.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private static String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        String name = extractFilename(filename);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
