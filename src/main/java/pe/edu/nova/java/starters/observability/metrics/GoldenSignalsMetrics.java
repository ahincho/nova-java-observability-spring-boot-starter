package pe.edu.nova.java.starters.observability.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import pe.edu.nova.java.libs.observability.GoldenSignalsRecorder;
import pe.edu.nova.java.libs.observability.MetricNames;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementación de {@link GoldenSignalsRecorder} usando la API de Micrometer.
 *
 * <p>Registra las Four Golden Signals:</p>
 * <ul>
 *   <li><b>Latencia:</b> Timer con percentiles (p50, p95, p99)</li>
 *   <li><b>Tráfico:</b> Counter con tags method, uri, status</li>
 *   <li><b>Errores:</b> Counter con tags method, uri, status, errorType</li>
 *   <li><b>Saturación:</b> Gauge de peticiones activas y ratio de heap JVM</li>
 * </ul>
 */
public class GoldenSignalsMetrics implements GoldenSignalsRecorder {

    private final MeterRegistry registry;
    private final AtomicInteger activeRequests = new AtomicInteger(0);

    /**
     * Crea una nueva instancia y registra los gauges de saturación.
     *
     * @param registry el registro de métricas de Micrometer
     */
    public GoldenSignalsMetrics(MeterRegistry registry) {
        this.registry = registry;

        // Gauge de peticiones activas concurrentes
        Gauge.builder(MetricNames.ACTIVE_REQUESTS, activeRequests, AtomicInteger::get)
                .description("Peticiones HTTP activas concurrentes")
                .register(registry);

        // Gauge de ratio de heap JVM usado (0 a 1)
        Gauge.builder(MetricNames.HEAP_RATIO, this, GoldenSignalsMetrics::heapRatio)
                .description("Ratio de heap JVM usado (0 a 1)")
                .register(registry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordLatency(String method, String uri, long durationNanos) {
        Timer.builder(MetricNames.LATENCY)
                .description("Latencia de peticiones HTTP")
                .tag("method", method)
                .tag("uri", uri)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(Duration.ofNanos(durationNanos));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordTraffic(String method, String uri, int statusCode) {
        Counter.builder(MetricNames.TRAFFIC)
                .description("Total de peticiones HTTP recibidas")
                .tag("method", method)
                .tag("uri", uri)
                .tag("status", String.valueOf(statusCode))
                .register(registry)
                .increment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void recordError(String method, String uri, int statusCode, String errorType) {
        Counter.builder(MetricNames.ERRORS)
                .description("Peticiones HTTP que resultaron en error")
                .tag("method", method)
                .tag("uri", uri)
                .tag("status", String.valueOf(statusCode))
                .tag("errorType", errorType)
                .register(registry)
                .increment();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementActive() {
        activeRequests.incrementAndGet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementActive() {
        activeRequests.decrementAndGet();
    }

    /**
     * Calcula el ratio de heap JVM usado (memoria usada / memoria máxima).
     *
     * @return valor entre 0.0 y 1.0
     */
    private double heapRatio() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        if (maxMemory == Long.MAX_VALUE) {
            return 0.0;
        }
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        return (double) usedMemory / maxMemory;
    }
}
