package pe.edu.nova.java.starters.observability.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuración de correlación de logs del starter de observabilidad.
 *
 * <p>Configura la inyección automática de traceId y spanId en el MDC de SLF4J
 * para cada petición procesada. Soporta Logback, Log4j2 y SLF4J sin forzar
 * un framework de logging específico.</p>
 *
 * <p>La correlación de logs se logra a través del bridge de OpenTelemetry
 * que automáticamente inyecta el contexto de traza en el MDC cuando
 * {@code opentelemetry-spring-boot-starter} está presente.</p>
 *
 * <p>Se activa cuando {@code nova.observability.logs.enabled=true} (por defecto).</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "nova.observability.logs",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LogCorrelationAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LogCorrelationAutoConfiguration.class);

    /**
     * Crea la configuración de correlación de logs.
     * La inyección de traceId/spanId en MDC es manejada automáticamente
     * por el opentelemetry-spring-boot-starter cuando está presente.
     */
    public LogCorrelationAutoConfiguration() {
        log.debug("Correlación de logs habilitada — traceId y spanId se inyectarán en MDC automáticamente");
    }
}
