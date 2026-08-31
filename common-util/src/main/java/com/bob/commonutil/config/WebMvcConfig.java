package com.bob.commonutil.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration for registering interceptors.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final MultipartFileValidationInterceptor fileValidationInterceptor;

    @Autowired
    public WebMvcConfig(MultipartFileValidationInterceptor fileValidationInterceptor) {
        this.fileValidationInterceptor = fileValidationInterceptor;
    }

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Register file validation interceptor for all paths
        registry.addInterceptor(fileValidationInterceptor)
                .addPathPatterns("/**");
    }
}
