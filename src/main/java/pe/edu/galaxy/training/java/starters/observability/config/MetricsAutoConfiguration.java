package pe.edu.nova.java.starters.observability.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import pe.edu.nova.java.starters.observability.filter.GoldenSignalsFilter;
import pe.edu.nova.java.starters.observability.filter.UriNormalizer;
import pe.edu.nova.java.starters.observability.metrics.GoldenSignalsMetrics;

/**
 * Auto-configuración de métricas del starter de observabilidad.
 *
 * <p>Registra los beans necesarios para el registro de Four Golden Signals:
 * {@link UriNormalizer}, {@link GoldenSignalsMetrics} y {@link GoldenSignalsFilter}.</p>
 *
 * <p>Se activa cuando {@code nova.observability.metrics.enabled=true} (por defecto).</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "nova.observability.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class MetricsAutoConfiguration {

    /**
     * Registra el normalizador de URIs basado en los mappings de Spring MVC.
     *
     * @param handlerMapping el handler mapping de Spring MVC
     * @return instancia de {@link UriNormalizer}
     */
    @Bean
    @ConditionalOnBean(RequestMappingHandlerMapping.class)
    public UriNormalizer uriNormalizer(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        return new UriNormalizer(handlerMapping);
    }

    /**
     * Registra la implementación de métricas Golden Signals con Micrometer.
     *
     * @param registry el registro de métricas de Micrometer
     * @return instancia de {@link GoldenSignalsMetrics}
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "nova.observability.metrics.golden-signals",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public GoldenSignalsMetrics goldenSignalsMetrics(MeterRegistry registry) {
        return new GoldenSignalsMetrics(registry);
    }

    /**
     * Registra el filtro HTTP que captura las Four Golden Signals.
     *
     * @param metrics       implementación de métricas Golden Signals
     * @param properties    propiedades de configuración
     * @param uriNormalizer normalizador de URIs
     * @return instancia de {@link GoldenSignalsFilter}
     */
    @Bean
    @ConditionalOnBean({GoldenSignalsMetrics.class, UriNormalizer.class})
    public GoldenSignalsFilter goldenSignalsFilter(GoldenSignalsMetrics metrics,
                                                   ObservabilityProperties properties,
                                                   UriNormalizer uriNormalizer) {
        return new GoldenSignalsFilter(metrics, properties, uriNormalizer);
    }
}
