package pe.edu.nova.java.starters.observability.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.PathContainer;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Normaliza URIs usando los path patterns declarados en Spring MVC.
 *
 * <p>En lugar de usar regex para adivinar qué segmentos son IDs,
 * consulta los {@code @RequestMapping} registrados para obtener
 * el pattern exacto (ej: {@code /api/users/{userId}/orders/{orderId}}).</p>
 *
 * <p>Esto evita falsos positivos como normalizar {@code classroom-dashboard}
 * a {@code {id}} por tener un guión.</p>
 *
 * <p>Usa un cache interno ({@link ConcurrentHashMap}) para evitar resolver
 * el pattern en cada petición.</p>
 */
public final class UriNormalizer {

    private final RequestMappingHandlerMapping handlerMapping;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    /**
     * Crea un nuevo normalizador basado en los mappings de Spring MVC.
     *
     * @param handlerMapping el handler mapping de Spring MVC con todos los endpoints registrados
     */
    public UriNormalizer(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    /**
     * Normaliza la URI buscando el path pattern declarado en Spring MVC.
     * Si encuentra un match, retorna el pattern (ej: {@code /api/users/{id}}).
     * Si no encuentra match, retorna la URI original sin modificar.
     *
     * @param request la petición HTTP
     * @return URI normalizada (pattern de Spring MVC) o la URI original
     */
    public String normalize(HttpServletRequest request) {
        String rawUri = request.getRequestURI();
        if (rawUri == null || rawUri.isEmpty()) {
            return "/";
        }
        return cache.computeIfAbsent(rawUri, this::resolvePattern);
    }

    /**
     * Busca el path pattern registrado que coincide con la URI.
     */
    private String resolvePattern(String uri) {
        Set<RequestMappingInfo> mappings = handlerMapping.getHandlerMethods().keySet();

        for (RequestMappingInfo mapping : mappings) {
            if (mapping.getPathPatternsCondition() == null) {
                continue;
            }
            Set<PathPattern> patterns = mapping.getPathPatternsCondition().getPatterns();
            for (PathPattern pattern : patterns) {
                if (pattern.matches(PathContainer.parsePath(uri))) {
                    return pattern.getPatternString();
                }
            }
        }

        // Si no hay match (ruta no mapeada), retornar URI original
        return uri;
    }
}
