package com.bob.jobportal.controller;

import com.bob.commonutil.exception.CommonException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("${recruiter.api.base.path}/homecontroller")
@PreAuthorize("hasAnyAuthority('ADMIN', 'RECRUITER', 'COMMITEE_MEMBER', 'COMMITTEE_MEMBER', 'ZONAL_HR')")
public class HomeController {

    @Value("${log.file.path}")
    private String logFilePathValue;

    @GetMapping("/")
    public String home() {
        return "BOB-Recruiter-app works";
    }

    @GetMapping(value = "/log", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Resource> getLogFile() {
        try {
            Path logFilePath = Paths.get(logFilePathValue);
            Resource resource = new FileSystemResource(logFilePath);

            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok().body(resource);
            }
        } catch (Exception e) {
            // Log the exception if necessary
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/test/exception")
    public void testExc() {
        throw new CommonException("naveen test");
    }

    @Value("${first-test-key:}")
    private String secretApiKey;

}
