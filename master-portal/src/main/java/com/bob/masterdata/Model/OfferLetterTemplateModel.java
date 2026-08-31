package com.bob.masterdata.Model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OfferLetterTemplateModel {
    private String templateName;
    private byte[] previewContent;
}
