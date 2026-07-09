package pe.edu.nova.java.starters.observability.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.trace.Tracer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.edu.nova.java.starters.observability.aop.MeteredAspect;
import pe.edu.nova.java.starters.observability.aop.TracedAspect;

/**
 * Auto-configuración de trazas distribuidas del starter de observabilidad.
 *
 * <p>Registra los aspectos AOP para las anotaciones {@code @Traced} y {@code @Metered},
 * y configura el sampling ratio desde las propiedades.</p>
 *
 * <p>Se activa cuando {@code nova.observability.traces.enabled=true} (por defecto).</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "nova.observability.traces",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class TracingAutoConfiguration {

    /**
     * Registra el aspecto AOP para la anotación {@code @Traced}.
     *
     * @param tracer el tracer de OpenTelemetry
     * @return instancia de {@link TracedAspect}
     */
    @Bean
    @ConditionalOnBean(Tracer.class)
    public TracedAspect tracedAspect(Tracer tracer) {
        return new TracedAspect(tracer);
    }

    /**
     * Registra el aspecto AOP para la anotación {@code @Metered}.
     *
     * @param registry el registro de métricas de Micrometer
     * @return instancia de {@link MeteredAspect}
     */
    @Bean
    @ConditionalOnBean(MeterRegistry.class)
    public MeteredAspect meteredAspect(MeterRegistry registry) {
        return new MeteredAspect(registry);
    }
}
