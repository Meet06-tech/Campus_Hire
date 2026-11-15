package com.placementmanagement.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.core.env.Environment;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationDatabaseGuard {
    @Bean
    static BeanFactoryPostProcessor testDatabaseOnlyGuard(Environment environment) {
        return beanFactory -> {
            String url = environment.getProperty("spring.datasource.url", "");
            if (!url.matches("jdbc:postgresql://(?:127\\.0\\.0\\.1|localhost):5432/placement_management_test(?:\\?.*)?")) {
                throw new IllegalStateException(
                        "Integration tests may connect only to placement_management_test; resolved URL was rejected");
            }
        };
    }
}
