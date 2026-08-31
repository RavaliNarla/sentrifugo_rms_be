package com.bob.commonutil.service;

import com.bob.commonutil.util.TempDirUtil;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
public class OcrService {


    public String extractTextFromFile(MultipartFile file) throws TesseractException, IOException {
        Path tempFilePath = null;
        String text = "";
        try {
             tempFilePath = convertMultiPartToFile(file);
            File tessDataFolder = loadTessData();

            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(tessDataFolder.getAbsolutePath());

             text = tesseract.doOCR(tempFilePath.toFile());
        }finally {
            TempDirUtil.deleteIfExists(tempFilePath);
        }

        return text;
    }

    private Path convertMultiPartToFile(MultipartFile file) throws IOException {

            // Create temp file with appropriate extension
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("File name cannot be null");
        }

        int dotIndex = filename.lastIndexOf('.');
        String extension = (dotIndex > 0) ? filename.substring(dotIndex) : null;
        Path convFilePath = TempDirUtil.createSecureTempFile("temp_ocr",extension);
        file.transferTo(convFilePath);
        return convFilePath;
    }

    private File loadTessData(){
        File tempTessDataFolder = new File(System.getProperty("java.io.tmpdir"), "tessdata");
        if (!tempTessDataFolder.exists()) {
            tempTessDataFolder.mkdirs();
        }
        
        // List of traineddata files to copy. You should add all the files you need here.
        // For now, I'm assuming 'eng.traineddata' is present.
        String[] dataFiles = {"eng.traineddata"}; 

        for (String dataFile : dataFiles) {
            File targetFile = new File(tempTessDataFolder, dataFile);
            if (!targetFile.exists()) {
                try (InputStream inputStream = new ClassPathResource("tessdata/" + dataFile).getInputStream()) {
                    Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    // Handle case where file might not exist in resources yet
                    log.error("Could not copy " + dataFile + " from resources: " + e.getMessage());
                }
            }
        }
        
        return tempTessDataFolder;
    }
}
