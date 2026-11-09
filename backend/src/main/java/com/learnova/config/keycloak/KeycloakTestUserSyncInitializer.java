package com.learnova.config.keycloak;

import com.learnova.service.KeycloakTestUserSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class KeycloakTestUserSyncInitializer {

    @Bean
    @ConditionalOnProperty(prefix = "keycloak.sync-test-users", name = "enabled", havingValue = "true")
    CommandLineRunner syncKeycloakTestUsers(KeycloakTestUserSyncService syncService) {
        return args -> {
            int createdUsers = syncService.syncMissingTestUsers();
            log.info("Imported {} missing Keycloak test user(s) into Learnova", createdUsers);
        };
    }
}
