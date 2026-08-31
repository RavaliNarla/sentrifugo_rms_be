package com.bob.commonutil.config;

import com.bob.commonutil.util.AppConstants;
import com.bob.commonutil.util.FileContentTypeUtil;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Hardens {@code ResponseEntity<byte[]>} responses for VAPT download guidance without
 * changing inline preview flows (PDF template preview, etc.).
 *
 * <p>Spring unwraps {@code ResponseEntity} before invoking advice, so the body parameter here
 * is the raw {@code byte[]} and response headers are taken from {@link ServerHttpResponse}.
 */
@ControllerAdvice
public class SecureBinaryDownloadResponseAdvice implements ResponseBodyAdvice<byte[]> {

    @Override
    public boolean supports(
            @NonNull MethodParameter returnType,
            @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        if (!ResponseEntity.class.isAssignableFrom(returnType.getParameterType())) {
            return false;
        }
        Class<?> bodyType = ResolvableType.forMethodParameter(returnType).getGeneric(0).resolve();
        return bodyType == byte[].class;
    }

    @Override
    public byte[] beforeBodyWrite(
            @Nullable byte[] body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response) {

        if (body == null) {
            return null;
        }

        HttpHeaders headers = response.getHeaders();
        String filename = resolveFilename(headers);
        boolean inline = isInlineDisposition(headers);

        if (!inline) {
            headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        }

        MediaType resolved = FileContentTypeUtil.resolveMediaType(filename, headers.getContentType());
        if (resolved != null) {
            headers.setContentType(resolved);
        } else if (headers.getContentType() == null) {
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        }

        if (!headers.containsKey(SecurityHeadersFilter.X_CONTENT_TYPE_OPTIONS)) {
            headers.add(SecurityHeadersFilter.X_CONTENT_TYPE_OPTIONS, SecurityHeadersFilter.NOSNIFF);
        }

        return body;
    }

    private static boolean isInlineDisposition(HttpHeaders headers) {
        String disposition = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        return disposition != null
                && disposition.toLowerCase().startsWith(AppConstants.CONTENT_DISPOSITION_INLINE);
    }

    private static String resolveFilename(HttpHeaders headers) {
        ContentDisposition disposition = headers.getContentDisposition();
        if (disposition != null && disposition.getFilename() != null) {
            return disposition.getFilename();
        }

        String raw = headers.getFirst(HttpHeaders.CONTENT_DISPOSITION);
        if (raw == null) {
            return "download";
        }

        int filenameIndex = raw.toLowerCase().indexOf("filename=");
        if (filenameIndex < 0) {
            return "download";
        }

        String value = raw.substring(filenameIndex + "filename=".length()).trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            return value.substring(1, value.length() - 1);
        }
        int semicolon = value.indexOf(';');
        return semicolon > 0 ? value.substring(0, semicolon).trim() : value;
    }
}
