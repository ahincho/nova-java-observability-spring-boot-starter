package pe.edu.nova.java.starters.observability.aop;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import pe.edu.nova.java.libs.observability.annotation.Traced;

/**
 * Aspecto AOP que procesa la anotación {@link Traced}.
 *
 * <p>Crea un span de OpenTelemetry que envuelve la ejecución del método anotado.
 * Si el método lanza una excepción, se registra en el span antes de re-lanzarla.</p>
 *
 * <p>El nombre del span se determina por:</p>
 * <ul>
 *   <li>El valor del atributo {@code value} de la anotación, si no está vacío</li>
 *   <li>El nombre del método, si {@code value} está vacío</li>
 * </ul>
 */
@Aspect
public class TracedAspect {

    private final Tracer tracer;

    /**
     * Crea una nueva instancia del aspecto de trazas.
     *
     * @param tracer el tracer de OpenTelemetry para crear spans
     */
    public TracedAspect(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * Advice que envuelve la ejecución de métodos anotados con {@link Traced}.
     *
     * @param joinPoint el punto de unión del método interceptado
     * @return el resultado de la ejecución del método
     * @throws Throwable si el método lanza una excepción
     */
    @Around("@annotation(pe.edu.nova.java.libs.observability.annotation.Traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Traced annotation = signature.getMethod().getAnnotation(Traced.class);

        String spanName = annotation.value().isEmpty()
                ? signature.getMethod().getName()
                : annotation.value();

        Span span = tracer.spanBuilder(spanName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            return joinPoint.proceed();
        } catch (Throwable t) {
            span.setStatus(StatusCode.ERROR, t.getMessage());
            span.recordException(t);
            throw t;
        } finally {
            span.end();
        }
    }
}
