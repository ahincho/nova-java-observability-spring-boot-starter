plugins {
    `java-library`
    `maven-publish`
    checkstyle
    id("net.nemerosa.versioning") version "4.0.1"
    id("signing")
}

versioning {
    releaseMode = "snapshot"
    displayMode = "snapshot"
    releaseBuild = false
}

group = "pe.edu.nova.java.starters"
version = findProperty("version") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://repo.spring.io/milestone")
    }
    // Internal Nova Platform dependency (nova-observability-utils lives in its own repo/package).
    // GITHUB_TOKEN cannot read packages from another repo, so this needs a PAT
    // (falls back to GITHUB_TOKEN for local/manual builds where only that is set).
    maven {
        name = "NovaObservabilityUtils"
        url = uri("https://maven.pkg.github.com/ahincho/nova-java-observability-utils")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("NOVA_PACKAGES_READ_TOKEN") ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

val springBootVersion = "4.0.5"
val otelInstrumentationVersion = "2.27.0"

dependencies {
    // BOMs — applied to all configurations via api (propagates to implementation, compileOnly, etc.)
    api(platform("org.springframework.boot:spring-boot-dependencies:$springBootVersion"))
    api(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:$otelInstrumentationVersion"))

    // Annotation processor needs explicit version since platform doesn't apply to annotationProcessor config
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:$springBootVersion")

    // Librería pura (transitiva al consumidor)
    api("pe.edu.nova.java.libs:nova-observability-utils:1.0.0")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")

    // Spring Web MVC (for RequestMappingHandlerMapping and OncePerRequestFilter)
    compileOnly("org.springframework.boot:spring-boot-starter-webmvc")

    // OpenTelemetry SDK (modo manual)
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter") {
        exclude(group = "io.opentelemetry.contrib", module = "opentelemetry-resource-providers")
        exclude(group = "io.opentelemetry.contrib", module = "opentelemetry-azure-resources")
    }

    // Micrometer → OTel bridge
    implementation("io.micrometer:micrometer-tracing-bridge-otel")

    // Jakarta Servlet (para el filtro)
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

checkstyle {
    // Only lint production code. Test suites commonly rely on static-import
    // wildcards (org.junit.jupiter.api.Assertions.*, net.jqwik.api.*), which
    // is an accepted convention that would otherwise trip AvoidStarImport.
    sourceSets = listOf(project.sourceSets.main.get())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/ahincho/nova-java-observability-spring-boot-starter")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    val gpgKeyId: String? = System.getenv("GPG_SIGNING_KEY_ID")
    val gpgKey: String? = System.getenv("GPG_SIGNING_KEY")
    val gpgPassword: String? = System.getenv("GPG_SIGNING_PASSWORD")

    if (gpgKeyId != null && gpgKey != null) {
        useInMemoryPgpKeys(gpgKeyId, gpgKey, gpgPassword ?: "")
        sign(publishing.publications)
    }
}