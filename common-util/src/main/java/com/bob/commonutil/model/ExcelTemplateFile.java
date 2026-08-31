package com.bob.commonutil.model;

import lombok.Data;

@Data
public class ExcelTemplateFile {
    String fileName;
    byte[] fileContent;
}
