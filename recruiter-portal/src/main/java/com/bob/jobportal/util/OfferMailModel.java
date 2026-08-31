package com.bob.jobportal.util;

import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class OfferMailModel {
    private String email;
    private String htmlBody;
    private MultipartFile pdfFile;
}
