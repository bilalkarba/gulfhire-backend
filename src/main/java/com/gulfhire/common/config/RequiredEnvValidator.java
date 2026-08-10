package com.gulfhire.common.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pre-startup validation of required secrets. Runs in {@code main()} BEFORE
 * Spring Boot starts, so a missing or unset secret produces ONE clear message
 * instead of cryptic errors such as
 * "password authentication failed for user ${DB_USERNAME}" or
 * "Could not resolve placeholder 'JWT_SECRET_KEY'".
 *
 * <p>It reads the same sources Spring will use — OS environment variables
 * first (highest precedence), then a local {@code .env} file from the current
 * working directory or the {@code gulfhire-backend/} folder.
 */
public final class RequiredEnvValidator {

    private static final String[] REQUIRED = {
            "DB_USERNAME",
            "DB_PASSWORD",
            "JWT_SECRET_KEY",
            "CLOUDINARY_CLOUD_NAME",
            "CLOUDINARY_API_KEY",
            "CLOUDINARY_API_SECRET"
    };

    private RequiredEnvValidator() {
    }

    /** Prints a clear message and exits when a required secret is missing. */
    public static void validateOrExit() {
        Map<String, String> env = new HashMap<>(System.getenv());
        loadEnvFile(Paths.get(".env"), env);
        loadEnvFile(Paths.get("gulfhire-backend/.env"), env);

        List<String> missing = new ArrayList<>();
        for (String name : REQUIRED) {
            String value = env.get(name);
            if (value == null || value.isBlank()
                    || value.contains("change_me") || value.startsWith("your_")) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            System.err.println("""
                    ============================================================
                    GulfHire backend cannot start — missing required secrets.
                    Missing or unset: %s

                    Fix: create the file gulfhire-backend/.env by copying
                    gulfhire-backend/.env.example and filling in your real
                    values (or set these as OS/IDE environment variables).
                    See gulfhire-backend/ENV.md for details.
                    ============================================================"""
                    .formatted(String.join(", ", missing)));
            System.exit(1);
        }
    }

    /**
     * Loads {@code KEY=VALUE} lines from a {@code .env} file. OS environment
     * variables already in the map are never overwritten (OS wins), which
     * matches Spring's own precedence rules.
     */
    private static void loadEnvFile(Path path, Map<String, String> env) {
        if (!Files.isReadable(path)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (i == 0 && line.startsWith("\uFEFF")) { // strip UTF-8 BOM
                    line = line.substring(1).trim();
                }
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = line.substring(0, eq).trim();
                if (key.isEmpty() || env.containsKey(key)) {
                    continue;
                }
                env.put(key, line.substring(eq + 1).trim());
            }
        } catch (IOException ignored) {
            // Best effort — Spring will surface any real config problems.
        }
    }
}
