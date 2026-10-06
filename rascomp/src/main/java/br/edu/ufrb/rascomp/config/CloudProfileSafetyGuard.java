package br.edu.ufrb.rascomp.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Impede que um ambiente cloud seja iniciado com massa de teste/seeds de QA.
 * Em vez de contaminar um banco persistente, a aplicação falha no startup.
 */
@Component
@Profile("cloud")
@RequiredArgsConstructor
public class CloudProfileSafetyGuard implements ApplicationRunner {

    private static final List<String> FORBIDDEN_FLAGS = List.of(
            "rascomp.seed.base",
            "rascomp.seed.postman",
            "rascomp.test-data.bracket-history-enabled",
            "rascomp.test-data.follow-line-enabled",
            "rascomp.test-data.demo-showcase-enabled",
            "rascomp.test-data.block3-validation-enabled",
            "rascomp.test-data.block4-portal-validation-enabled"
    );

    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        boolean testdataProfile = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "testdata".equalsIgnoreCase(profile));

        if (testdataProfile) {
            throw new IllegalStateException(
                    "Profile cloud não pode ser combinado com testdata. Use um banco cloud limpo.");
        }

        List<String> enabledFlags = FORBIDDEN_FLAGS.stream()
                .filter(flag -> environment.getProperty(flag, Boolean.class, false))
                .toList();

        if (!enabledFlags.isEmpty()) {
            throw new IllegalStateException(
                    "Profile cloud bloqueou seeds/testdata habilitados: " + String.join(", ", enabledFlags));
        }
    }
}
