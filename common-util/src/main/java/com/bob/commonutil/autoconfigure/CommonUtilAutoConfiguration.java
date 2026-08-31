package com.bob.commonutil.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Auto-configuration for common-util module.
 * Registers services contained in common-util so that
 * applications that depend on this library don't need to add
 * explicit @ComponentScan for these packages.
 */
@AutoConfiguration
@ComponentScan(basePackages = {
        "com.bob.commonutil.service",
        "com.bob.commonutil.config",
        "com.bob.commonutil.util",
        "com.bob.commonutil.exception"
})
public class CommonUtilAutoConfiguration {
}
