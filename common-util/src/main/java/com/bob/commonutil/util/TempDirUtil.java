package com.bob.commonutil.util;

import com.bob.commonutil.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;


@Slf4j
public class TempDirUtil {

    private static final String DIR_NAME = "myapp_secure";
    private  static Path cachedDir;

    public static Path getSecureTempDir() {
        // Return cached if already created
        if (cachedDir != null) {
            return cachedDir;
        }

        Path baseTemp = Paths.get(System.getProperty("java.io.tmpdir"))
                .toAbsolutePath()
                .normalize();

        log.info("Base Folder : {}", baseTemp);

        Path tempDir = baseTemp.resolve(DIR_NAME).normalize();

        log.info("Created Dir : {}", tempDir);

        try {

            Files.createDirectories(tempDir);

            Path realBase = baseTemp.toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path realDir = tempDir.toRealPath(LinkOption.NOFOLLOW_LINKS);

            log.info("Real Base : {}", realBase);
            log.info("Real Dir : {}", realDir);

            if (!realDir.startsWith(realBase)) {
                throw new CommonException("Invalid temporary directory.");
            }

            applySecurePermissions(realDir, true);

            cachedDir = realDir;

            log.info("Temp Dir : {}", realDir);

            return realDir;

        } catch (IOException e) {
            log.error("Some error occurred while creating directory", e);
            throw new CommonException("Failed to create directory.");
        }
    }

    public static Path createSecureTempFile(String prefix, String extension) {
        try {

            //by any chance if prefix and extension are null
            String safePrefix = (prefix == null) ? "tmpFile" : prefix;
            String safeExtension = (extension == null) ? ".tmp" : extension;

            Path dir = getSecureTempDir();
            Path tempFile = Files.createTempFile(dir, safePrefix, safeExtension);

            Path realDir = dir.toRealPath(LinkOption.NOFOLLOW_LINKS);
            Path realFile = tempFile.toRealPath(LinkOption.NOFOLLOW_LINKS);
            log.info("Real Dir: {}",realDir);
            log.info("Real File: {}",realFile);

            // Ensure file is actually inside our secure directory
            if (!realFile.startsWith(realDir)) {
                Files.deleteIfExists(tempFile);
                throw new CommonException("Invalid temporary file location.");
            }
            applySecurePermissions(tempFile,false);
            return realFile;
        } catch (IOException e) {
            log.error("Error creating temp file: {}", e.getMessage(), e);
            throw new CommonException("Failed to create temp file");
        }

    }
    private static void applySecurePermissions(Path path, boolean isDirectory) {
        try {
            if (!Files.getFileStore(path).supportsFileAttributeView("posix")) {
                return;
            }

            String permissions = isDirectory ? "rwx------" : "rw-------";

            Files.setPosixFilePermissions(
                    path,
                    PosixFilePermissions.fromString(permissions)
            );

        } catch (IOException | UnsupportedOperationException e) {
            log.debug("Skipping POSIX permissions: {}", e.getMessage());
        }
    }

    public static void deleteIfExists(Path path) {
        if (path != null) {
            try {
                boolean deleted = Files.deleteIfExists(path);
                if (!deleted) {
                    log.warn("Failed to delete temporary OCR file: {}", path.toAbsolutePath());
                }
            } catch (IOException e) {
                log.error("Failed to delete image : {}", e.getMessage());
            }
        }
    }
}