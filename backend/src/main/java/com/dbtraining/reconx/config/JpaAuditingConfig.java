package com.dbtraining.reconx.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * TICKET-ADV050 — @CreatedDate / @LastModifiedDate population.
 *
 * Kept off {@code ReconxApplication} deliberately: {@code @WebMvcTest} always
 * loads the {@code @SpringBootApplication} class as its root config, and
 * {@code @EnableJpaAuditing} there pulls in a JpaMetamodelMappingContext that
 * a controller slice (no entity scanning) can't satisfy. A dedicated
 * {@code @Configuration} class isn't picked up by the slice, so it stays out
 * of the way of {@code @WebMvcTest}.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
