package com.pegacorn.rently.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Configuration
public class FileConfig implements WebMvcConfigurer {

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(uploadPath).toAbsolutePath().normalize();
        String uploadLocation = uploadDir.toUri().toString();

        if (!uploadLocation.endsWith("/")) {
            uploadLocation += "/";
        }

        log.info("Mapping /uploads/** to {}", uploadLocation);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadLocation);
    }
}
