package pe.edu.nova.java.starters.observability.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Auto-configuración del exportador OTLP del starter de observabilidad.
 *
 * <p>Configura el endpoint, protocolo y timeout de exportación OTLP
 * a partir de las propiedades del starter, mapeándolas a las propiedades
 * estándar de OpenTelemetry Spring Boot Starter.</p>
 *
 * <p>Exporta traces, métricas y logs exclusivamente vía protocolo OTLP
 * al OpenTelemetry Collector configurado.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "nova.observability",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OtlpExporterAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(OtlpExporterAutoConfiguration.class);

    /**
     * Configura las propiedades de exportación OTLP mapeando las propiedades
     * del starter a las propiedades estándar de OpenTelemetry.
     *
     * @param properties  propiedades de configuración del starter
     * @param environment el entorno de Spring para agregar propiedades
     */
    public OtlpExporterAutoConfiguration(ObservabilityProperties properties,
                                         ConfigurableEnvironment environment) {
        Map<String, Object> otelProps = new HashMap<>();

        String endpoint = properties.getOtlp().getEndpoint();
        String protocol = properties.getOtlp().getProtocol();
        long timeoutMs = properties.getOtlp().getTimeout().toMillis();

        // Mapear propiedades del starter a propiedades de OpenTelemetry
        otelProps.put("otel.exporter.otlp.endpoint", endpoint);
        otelProps.put("otel.exporter.otlp.protocol", protocol);
        otelProps.put("otel.exporter.otlp.timeout", String.valueOf(timeoutMs));

        // Configurar sampling ratio
        double samplingRatio = properties.getTraces().getSamplingRatio();
        otelProps.put("otel.traces.sampler", "parentbased_traceidratio");
        otelProps.put("otel.traces.sampler.arg", String.valueOf(samplingRatio));

        // Agregar como property source con baja prioridad (permite override)
        MapPropertySource propertySource = new MapPropertySource(
                "nova-observability-otlp", otelProps);
        environment.getPropertySources().addLast(propertySource);

        log.debug("Exportación OTLP configurada — endpoint: {}, protocolo: {}, timeout: {}ms",
                endpoint, protocol, timeoutMs);
    }
}
