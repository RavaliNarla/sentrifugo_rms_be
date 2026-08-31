package com.bob.db.autoconfigure;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Auto-configuration for bob-db module.
 * Registers entities and repositories contained in bob-db so that
 * applications that depend on this library don't need to add
 * explicit @EntityScan/@EnableJpaRepositories for these packages.
 */
@AutoConfiguration
@EntityScan(basePackages = {
        "com.bob.db.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.bob.db.repository"
})
@ComponentScan(basePackages = {
        "com.bob.db.mapper",
        "com.bob.db.repository"
})
public class BobDbJpaAutoConfiguration {
}
