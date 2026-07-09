package pe.edu.nova.java.starters.observability;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import pe.edu.nova.java.starters.observability.config.LogCorrelationAutoConfiguration;
import pe.edu.nova.java.starters.observability.config.MetricsAutoConfiguration;
import pe.edu.nova.java.starters.observability.config.ObservabilityProperties;
import pe.edu.nova.java.starters.observability.config.OtlpExporterAutoConfiguration;
import pe.edu.nova.java.starters.observability.config.TracingAutoConfiguration;
import pe.edu.nova.java.starters.observability.health.CollectorHealthIndicator;

/**
 * Auto-configuración principal del starter de observabilidad.
 *
 * <p>Se activa cuando {@code nova.observability.enabled=true} (por defecto).
 * Importa todas las sub-configuraciones y registra el health check obligatorio
 * del OpenTelemetry Collector.</p>
 *
 * <p>Sub-configuraciones importadas:</p>
 * <ul>
 *   <li>{@link TracingAutoConfiguration} — trazas distribuidas y aspectos AOP</li>
 *   <li>{@link MetricsAutoConfiguration} — métricas Four Golden Signals</li>
 *   <li>{@link LogCorrelationAutoConfiguration} — correlación de logs con traceId/spanId</li>
 *   <li>{@link OtlpExporterAutoConfiguration} — exportación OTLP al Collector</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnProperty(
        prefix = "nova.observability",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableConfigurationProperties(ObservabilityProperties.class)
@Import({
        TracingAutoConfiguration.class,
        MetricsAutoConfiguration.class,
        LogCorrelationAutoConfiguration.class,
        OtlpExporterAutoConfiguration.class
})
public class ObservabilityAutoConfiguration {

    /**
     * Registra el indicador de salud obligatorio del OpenTelemetry Collector.
     * Este bean se registra siempre que la observabilidad esté habilitada,
     * sin condicional adicional.
     *
     * @param properties propiedades de configuración con el endpoint del Collector
     * @return instancia de {@link CollectorHealthIndicator}
     */
    @Bean
    public CollectorHealthIndicator collectorHealthIndicator(ObservabilityProperties properties) {
        return new CollectorHealthIndicator(properties);
    }
}
