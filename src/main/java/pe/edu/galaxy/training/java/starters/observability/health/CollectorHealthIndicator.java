package pe.edu.nova.java.starters.observability.health;

import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import pe.edu.nova.java.starters.observability.config.ObservabilityProperties;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Indicador de salud obligatorio que verifica la conectividad
 * con el OpenTelemetry Collector.
 *
 * <p>Reporta {@code UP} si el Collector responde con status &lt; 500,
 * {@code DOWN} si no está disponible o responde con error del servidor.</p>
 *
 * <p>Usa {@link HttpClient} de Java con timeout de conexión de 3 segundos
 * y timeout de respuesta de 5 segundos.</p>
 */
public class CollectorHealthIndicator extends AbstractHealthIndicator {

    private final String endpoint;
    private final HttpClient httpClient;

    /**
     * Crea una nueva instancia del indicador de salud del Collector.
     *
     * @param properties propiedades de configuración con el endpoint del Collector
     */
    public CollectorHealthIndicator(ObservabilityProperties properties) {
        super("No se puede conectar con el OpenTelemetry Collector");
        this.endpoint = properties.getOtlp().getEndpoint();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    /**
     * Ejecuta la verificación de salud contra el endpoint del Collector.
     *
     * @param builder el builder de Health para construir el resultado
     */
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<Void> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() < 500) {
                builder.up()
                        .withDetail("endpoint", endpoint)
                        .withDetail("statusCode", response.statusCode());
            } else {
                builder.down()
                        .withDetail("endpoint", endpoint)
                        .withDetail("statusCode", response.statusCode());
            }
        } catch (Exception e) {
            builder.down()
                    .withDetail("endpoint", endpoint)
                    .withDetail("error", e.getMessage());
        }
    }
}
