package com.bob.commonutil.config;

import com.bob.commonutil.service.FileService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Set;
import org.springframework.web.client.RestTemplate;

@Configuration
public class CommonUtilConfig {
    @Bean
    public FileService fileService() {
        return new FileService();
    }

    @Bean
    public SpringResourceTemplateResolver springResourceTemplateResolver(){
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);
        resolver.setOrder(1);
        resolver.setResolvablePatterns(Set.of("*"));
        return resolver;
    }

    @Bean
    public StringTemplateResolver stringTemplateResolver(){
        StringTemplateResolver resolver = new StringTemplateResolver();

        resolver.setTemplateMode("HTML");
        resolver.setCacheable(false);
        resolver.setOrder(2);

        return resolver;
    }


    /**
     * RestTemplate bean for HTTP client operations
     * Used by SmsService for making API calls to Bank of Baroda SMS gateway
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

