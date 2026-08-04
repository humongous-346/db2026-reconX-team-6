package com.dbtraining.reconx.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TICKET-ADV140 — Grafana "Consumer lag by topic" panel.
 * TICKET-ADV141 — Grafana "Kafka throughput" panel.
 * TICKET-ADV142 — Grafana "DLQ message count" panel + KafkaDlqMessages alert.
 *
 * These files aren't on the Java classpath (they're read by Grafana/
 * Prometheus directly), so this test locates them relative to the repo root
 * and validates them as plain JSON/YAML documents.
 */
class GrafanaDashboardConfigTest {

    private Path repoRoot() {
        // backend/ is always the Maven working directory for this module.
        return Paths.get("").toAbsolutePath().getParent();
    }

    @SuppressWarnings("unchecked")
    private JsonNode loadDashboard() throws Exception {
        File file = repoRoot()
                .resolve("monitoring/grafana/provisioning/dashboards/reconx-overview.json")
                .toFile();
        return new ObjectMapper().readTree(file);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> loadAlertRules() throws Exception {
        File file = repoRoot().resolve("monitoring/prometheus/alerts.yml").toFile();
        try (FileInputStream in = new FileInputStream(file)) {
            Map<String, Object> doc = new Yaml().load(in);
            List<Map<String, Object>> groups = (List<Map<String, Object>>) doc.get("groups");
            return (List<Map<String, Object>>) groups.get(0).get("rules");
        }
    }

    private JsonNode findPanel(JsonNode dashboard, String titleContains) {
        return StreamSupport.stream(dashboard.get("panels").spliterator(), false)
                .filter(p -> p.get("title").asText().contains(titleContains))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no panel titled like: " + titleContains));
    }

    @Test
    void consumerLagPanel_hasCorrectQueryAndThresholds() throws Exception {
        JsonNode panel = findPanel(loadDashboard(), "Consumer lag by topic");

        assertThat(panel.get("type").asText()).isEqualTo("timeseries");
        assertThat(panel.get("targets").get(0).get("expr").asText())
                .isEqualTo("sum by (topic) (kafka_consumer_records_lag)");

        JsonNode steps = panel.get("fieldConfig").get("defaults").get("thresholds").get("steps");
        List<Integer> thresholdValues = StreamSupport.stream(steps.spliterator(), false)
                .filter(s -> !s.get("value").isNull())
                .map(s -> s.get("value").asInt())
                .toList();
        assertThat(thresholdValues).containsExactly(100, 1000);
    }

    @Test
    void throughputPanel_comparesProducedAndConsumed() throws Exception {
        JsonNode panel = findPanel(loadDashboard(), "throughput");

        List<String> exprs = StreamSupport.stream(panel.get("targets").spliterator(), false)
                .map(t -> t.get("expr").asText())
                .toList();
        assertThat(exprs).anyMatch(e -> e.contains("kafka_consumer_records_consumed_total"));
        assertThat(exprs).anyMatch(e -> e.contains("kafka_producer_record_send_total"));
    }

    @Test
    void dlqCountPanel_isAStatPanelOnTheDlqTopic() throws Exception {
        JsonNode panel = findPanel(loadDashboard(), "DLQ message count");

        assertThat(panel.get("type").asText()).isEqualTo("stat");
        assertThat(panel.get("targets").get(0).get("expr").asText())
                .isEqualTo("sum(kafka_consumer_records_consumed_total{topic=\"trade-events-dlq\"})");
    }

    @Test
    void kafkaDlqMessagesAlert_criticalSeverityFiresOnAnyDlqActivity() throws Exception {
        Map<String, Object> alert = loadAlertRules().stream()
                .filter(r -> "KafkaDlqMessages".equals(r.get("alert")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("KafkaDlqMessages alert not found"));

        assertThat(alert.get("expr"))
                .isEqualTo("sum(kafka_consumer_records_consumed_total{topic=\"trade-events-dlq\"}) > 0");
        assertThat(alert.get("for")).isEqualTo("1m");
        Map<String, Object> labels = (Map<String, Object>) alert.get("labels");
        assertThat(labels.get("severity")).isEqualTo("critical");
        Map<String, Object> annotations = (Map<String, Object>) alert.get("annotations");
        assertThat((String) annotations.get("description")).contains("/api/v1/admin/dlq");
    }
}
