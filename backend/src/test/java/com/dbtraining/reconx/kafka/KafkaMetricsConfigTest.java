package com.dbtraining.reconx.kafka;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV139 — application.yml enables the Micrometer Kafka client-metrics
 * binder on both the Boot side and the Kafka client side, and exposes
 * /actuator/prometheus. Parses the YAML directly rather than booting a full
 * Spring context, since this ticket is pure configuration.
 */
class KafkaMetricsConfigTest {

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadApplicationYml() {
        try (InputStream in = getClass().getResourceAsStream("/application.yml")) {
            return new Yaml().load(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> path(Map<String, Object> root, String... keys) {
        Map<String, Object> current = root;
        for (String key : keys) {
            Object next = current.get(key);
            assertThat(next).as("expected key path %s", String.join(".", keys)).isNotNull();
            current = (Map<String, Object>) next;
        }
        return current;
    }

    @Test
    void kafkaMetricsBinderIsEnabled() {
        Map<String, Object> yml = loadApplicationYml();
        Map<String, Object> binders = path(yml, "management", "metrics", "binders", "kafka");
        assertThat(binders.get("enabled")).isEqualTo(true);
    }

    @Test
    void consumerPropertiesRegisterKafkaClientMetricsReporter() {
        Map<String, Object> yml = loadApplicationYml();
        Map<String, Object> consumerProps = path(yml, "spring", "kafka", "consumer", "properties");
        assertThat(consumerProps.get("metric.reporters"))
                .isEqualTo("io.micrometer.core.instrument.binder.kafka.KafkaClientMetrics");
    }

    @Test
    void prometheusIsExposedOnActuator() {
        Map<String, Object> yml = loadApplicationYml();
        Map<String, Object> exposure = path(yml, "management", "endpoints", "web", "exposure");
        String include = (String) exposure.get("include");
        List<String> endpoints = List.of(include.split(","));
        assertThat(endpoints).contains("prometheus");
    }
}
