package com.gulfhire;

import com.gulfhire.common.config.RequiredEnvValidator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class GulfhireBackendApplication {

    public static void main(String[] args) {
        // Fail fast with a clear message before Spring starts if required
        // secrets (DB, JWT, Cloudinary) are missing or unset.
        RequiredEnvValidator.validateOrExit();

        SpringApplication.run(GulfhireBackendApplication.class, args);
    }

}
