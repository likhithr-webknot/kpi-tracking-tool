package com.webknot.kpi.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionSettingsValidatorTest {

    @Test
    void run_throwsForInsecureProductionConfiguration() {
        ProductionSettingsValidator validator = new ProductionSettingsValidator(
                "change-me",
                false,
                "",
                true,
                true,
                true,
                List.of("http://localhost:3000")
        );

        assertThrows(IllegalStateException.class, () -> validator.run(new DefaultApplicationArguments(new String[0])));
    }

    @Test
    void run_allowsSecureProductionConfiguration() {
        ProductionSettingsValidator validator = new ProductionSettingsValidator(
                "this-is-a-very-long-production-jwt-secret-value-123456",
                true,
                "strong-password",
                false,
                false,
                false,
                List.of("https://kpi.example.com")
        );

        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments(new String[0])));
    }
}
