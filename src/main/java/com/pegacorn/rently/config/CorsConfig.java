package com.pegacorn.rently.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    private List<String> getOriginsList() {
        // If "*" is configured, return a pattern that matches all origins
        if ("*".equals(allowedOrigins.trim())) {
            return List.of("*");
        }
        return Arrays.asList(allowedOrigins.split(","));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if ("*".equals(allowedOrigins.trim())) {
            // For development: allow all origins without credentials restriction
            registry.addMapping("/**")
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                    .allowedHeaders("*")
                    .exposedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
        } else {
            registry.addMapping("/**")
                    .allowedOriginPatterns(getOriginsList().toArray(new String[0]))
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        if ("*".equals(allowedOrigins.trim())) {
            // For development: allow all origins
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOriginPatterns(getOriginsList());
        }
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
