package pe.edu.nova.java.starters.observability.aop;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import pe.edu.nova.java.libs.observability.annotation.Metered;

/**
 * Aspecto AOP que procesa la anotación {@link Metered}.
 *
 * <p>Registra la latencia (Timer) y el conteo de invocaciones para
 * cada método anotado con {@code @Metered}.</p>
 *
 * <p>El nombre de la métrica se determina por:</p>
 * <ul>
 *   <li>El valor del atributo {@code value} de la anotación, si no está vacío</li>
 *   <li>{@code NombreClase.nombreMetodo}, si {@code value} está vacío</li>
 * </ul>
 */
@Aspect
public class MeteredAspect {

    private final MeterRegistry registry;

    /**
     * Crea una nueva instancia del aspecto de métricas.
     *
     * @param registry el registro de métricas de Micrometer
     */
    public MeteredAspect(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Advice que envuelve la ejecución de métodos anotados con {@link Metered}.
     * Registra un Timer con la latencia del método y tags de clase y método.
     *
     * @param joinPoint el punto de unión del método interceptado
     * @return el resultado de la ejecución del método
     * @throws Throwable si el método lanza una excepción
     */
    @Around("@annotation(pe.edu.nova.java.libs.observability.annotation.Metered)")
    public Object meterMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Metered annotation = signature.getMethod().getAnnotation(Metered.class);

        String metricName = annotation.value().isEmpty()
                ? signature.getDeclaringType().getSimpleName() + "." + signature.getMethod().getName()
                : annotation.value();

        Timer.Sample sample = Timer.start(registry);
        try {
            return joinPoint.proceed();
        } finally {
            sample.stop(Timer.builder(metricName)
                    .description("Latencia del método anotado con @Metered")
                    .tag("class", signature.getDeclaringType().getSimpleName())
                    .tag("method", signature.getMethod().getName())
                    .register(registry));
        }
    }
}
