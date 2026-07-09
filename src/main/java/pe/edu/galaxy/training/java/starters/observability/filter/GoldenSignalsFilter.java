package pe.edu.nova.java.starters.observability.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.edu.nova.java.starters.observability.config.ObservabilityProperties;
import pe.edu.nova.java.starters.observability.metrics.GoldenSignalsMetrics;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro HTTP que registra automáticamente los Four Golden Signals
 * para cada petición.
 *
 * <p>Excluye rutas configuradas en {@code nova.observability.filter.exclude-paths}
 * y normaliza URIs usando los path patterns declarados en Spring MVC.</p>
 *
 * <p>Lógica del filtro:</p>
 * <ol>
 *   <li>Incrementar peticiones activas</li>
 *   <li>Iniciar temporizador</li>
 *   <li>Ejecutar la cadena de filtros</li>
 *   <li>Registrar latencia, tráfico y errores (si aplica)</li>
 *   <li>Decrementar peticiones activas</li>
 * </ol>
 *
 * <p>El filtro es resiliente: captura excepciones del registro de métricas
 * sin afectar la petición HTTP.</p>
 */
public class GoldenSignalsFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GoldenSignalsFilter.class);

    private final GoldenSignalsMetrics metrics;
    private final List<String> excludePaths;
    private final UriNormalizer uriNormalizer;

    /**
     * Crea una nueva instancia del filtro de Golden Signals.
     *
     * @param metrics       implementación de métricas Golden Signals
     * @param properties    propiedades de configuración del starter
     * @param uriNormalizer normalizador de URIs basado en Spring MVC
     */
    public GoldenSignalsFilter(GoldenSignalsMetrics metrics,
                               ObservabilityProperties properties,
                               UriNormalizer uriNormalizer) {
        this.metrics = metrics;
        this.excludePaths = properties.getFilter().getExcludePaths();
        this.uriNormalizer = uriNormalizer;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = uriNormalizer.normalize(request);

        if (isExcluded(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        metrics.incrementActive();
        long startNs = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            try {
                long durationNs = System.nanoTime() - startNs;
                String method = request.getMethod();
                int status = response.getStatus();

                metrics.recordLatency(method, uri, durationNs);
                metrics.recordTraffic(method, uri, status);

                String errorType = classifyError(status);
                if (errorType != null) {
                    metrics.recordError(method, uri, status, errorType);
                }
            } catch (Exception e) {
                log.warn("Error registrando métricas Golden Signals: {}", e.getMessage());
            } finally {
                metrics.decrementActive();
            }
        }
    }

    /**
     * Verifica si la URI está excluida del registro de métricas.
     *
     * @param uri la URI normalizada
     * @return {@code true} si la URI debe ser excluida
     */
    private boolean isExcluded(String uri) {
        return excludePaths.stream().anyMatch(uri::startsWith);
    }

    /**
     * Clasifica el código de estado HTTP en un tipo de error.
     *
     * @param status código de estado HTTP
     * @return tipo de error o {@code null} si no es un error
     */
    private static String classifyError(int status) {
        if (status == 503) return "service_unavailable";
        if (status == 429) return "rate_limited";
        if (status >= 500) return "server_error";
        if (status >= 400) return "client_error";
        return null;
    }
}
