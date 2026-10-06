package br.edu.ufrb.rascomp.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;

class CloudProfileSafetyGuardTest {

    @Test
    void deveAceitarCloudSemSeeds() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("cloud");

        CloudProfileSafetyGuard guard = new CloudProfileSafetyGuard(environment);

        assertDoesNotThrow(() -> guard.run(new DefaultApplicationArguments()));
    }

    @Test
    void deveBloquearProfileTestdataJuntoDoCloud() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("cloud", "testdata");

        CloudProfileSafetyGuard guard = new CloudProfileSafetyGuard(environment);

        assertThrows(IllegalStateException.class,
                () -> guard.run(new DefaultApplicationArguments()));
    }

    @Test
    void deveBloquearQualquerSeedDeQa() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("cloud");
        environment.setProperty("rascomp.seed.base", "true");

        CloudProfileSafetyGuard guard = new CloudProfileSafetyGuard(environment);

        assertThrows(IllegalStateException.class,
                () -> guard.run(new DefaultApplicationArguments()));
    }
}
