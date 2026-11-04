package com.learnova.config;

import com.learnova.service.KeycloakTestUserSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KeycloakTestUserSyncInitializer {

    private static final Logger logger =
            LoggerFactory.getLogger(KeycloakTestUserSyncInitializer.class);

    @Bean
    @ConditionalOnProperty(
            prefix = "keycloak.sync-test-users",
            name = "enabled",
            havingValue = "true"
    )
    CommandLineRunner syncKeycloakTestUsers(
            KeycloakTestUserSyncService syncService
    ) {
        return args -> {
            int createdUsers = syncService.syncMissingTestUsers();
            logger.info(
                    "Imported {} missing Keycloak test user(s) into Learnova",
                    createdUsers
            );
        };
    }
}
