package com.pegacorn.rently.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials-file:firebase-service-account.json}")
    private String credentialsFile;

    @PostConstruct
    public void initialize() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                InputStream serviceAccount = getCredentialsStream();

                if (serviceAccount != null) {
                    FirebaseOptions options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();

                    FirebaseApp.initializeApp(options);
                    log.info("Firebase initialized successfully");
                } else {
                    log.warn("Firebase credentials file not found. Push notifications will not work.");
                }
            } catch (IOException e) {
                log.error("Failed to initialize Firebase: {}", e.getMessage());
            }
        }
    }

    private InputStream getCredentialsStream() {
        // Try classpath first
        try {
            ClassPathResource resource = new ClassPathResource(credentialsFile);
            if (resource.exists()) {
                return resource.getInputStream();
            }
        } catch (IOException ignored) {
        }

        // Try file system
        try {
            return new FileInputStream(credentialsFile);
        } catch (IOException ignored) {
        }

        return null;
    }
}
