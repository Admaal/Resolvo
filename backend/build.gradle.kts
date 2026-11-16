plugins {
    java
    id("org.springframework.boot") version "3.5.16"
    id("info.solidsoft.pitest") version "1.19.0"
}

group = "com.admaal"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:3.5.16"))
    implementation(platform("org.springframework.ai:spring-ai-bom:1.1.8"))
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai")
    implementation("org.springframework.ai:spring-ai-starter-model-google-genai-embedding")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform {
        if (project.hasProperty("fast")) {
            excludeTags("full")
        }
        // Los evals llaman a Gemini real y gastan cuota: solo corren con la tarea `evals`.
        if (name != "evals") {
            excludeTags("eval")
        }
    }
    // El bucle rapido filtra por paquete; un paquete todavia sin tests no es un fallo.
    filter {
        isFailOnNoMatchingTests = false
    }
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.register<Test>("evals") {
    description = "Evals de IA contra Gemini real. Necesita RESOLVO_GEMINI_API_KEY y Docker."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("eval")
    }
    // Un eval cuesta cuota: nunca se da por hecho a partir de una cache.
    outputs.upToDateWhen { false }
    testLogging {
        showStandardStreams = true
    }
}

pitest {
    pitestVersion.set("1.30.1")
    junit5PluginVersion.set("1.2.3")
    targetClasses.set(listOf("com.admaal.resolvo.domain.*", "com.admaal.resolvo.application.*"))
    targetTests.set(listOf("com.admaal.resolvo.domain.*", "com.admaal.resolvo.application.*"))
    mutationThreshold.set(80)
    failWhenNoMutations.set(true)
    outputFormats.set(listOf("XML", "HTML"))
    timestampedReports.set(false)
    threads.set(4)
    // PIT 1.30 no incluye el almacén de historia. -PpitIncremental se acepta
    // y la pasada sigue siendo completa; el gate no depende de un plugin de pago.
}
