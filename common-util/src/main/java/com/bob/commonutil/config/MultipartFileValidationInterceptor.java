package com.bob.commonutil.config;

import com.bob.commonutil.util.FileValidationUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * Global interceptor that validates all uploaded files before reaching controllers.
 * Validates MIME types, file extensions, and sanitizes filenames for security.
 */
@Component
public class MultipartFileValidationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(MultipartFileValidationInterceptor.class);

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler){
        // Only process multipart requests
        if (request instanceof MultipartHttpServletRequest multipartRequest) {
            Map<String, MultipartFile> fileMap = multipartRequest.getFileMap();
            
            if (!fileMap.isEmpty()) {
                logger.debug("Validating {} uploaded file(s) for request: {}", fileMap.size(), request.getRequestURI());
                
                // Validate each uploaded file
                for (Map.Entry<String, MultipartFile> entry : fileMap.entrySet()) {
                    MultipartFile file = entry.getValue();
                    String paramName = entry.getKey();
                    
                    logger.debug("Processing multipart parameter '{}' with filename: {}", paramName, 
                            file != null ? file.getOriginalFilename() : "null");
                    
                    if (!FileValidationUtil.shouldValidateMultipartPart(file, paramName)) {
                        logger.debug("Skipping validation for non-file multipart parameter: {}", paramName);
                        continue;
                    }

                    // Validate file (throws InvalidFileTypeException if invalid)
                    FileValidationUtil.validateFile(file);
                    logger.debug("File '{}' from parameter '{}' passed validation", file.getOriginalFilename(), paramName);
                }
            }
        }
        
        return true;
    }
}
