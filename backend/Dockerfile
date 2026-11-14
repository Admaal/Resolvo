# API de Resolvo para Render Free (512 MB). Contexto de build: backend/.
# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
# El wrapper puede llegar con CRLF desde un checkout de Windows.
RUN sed -i 's/\r$//' gradlew && chmod +x gradlew
COPY src src
RUN ./gradlew --no-daemon bootJar -x test --console=plain

FROM eclipse-temurin:21-jre AS extraer
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --destination extraido
# Archivo CDS para arrancar antes. El entrenamiento no tiene base de datos: si falla,
# la imagen se construye igual, solo que sin CDS (-Xshare:auto lo ignora).
RUN java -XX:ArchiveClassesAtExit=extraido/app.jsa \
      -Dspring.context.exit=onRefresh \
      -Dspring.flyway.enabled=false \
      -Dspring.jpa.hibernate.ddl-auto=none \
      -Dspring.datasource.hikari.initialization-fail-timeout=-1 \
      -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false \
      -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect \
      -DRESOLVO_JWT_SECRET=entrenamiento-cds-no-es-un-secreto-real \
      -jar extraido/app.jar || echo "Sin CDS: el entrenamiento necesita base de datos."

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 resolvo
WORKDIR /app
COPY --from=extraer --chown=resolvo:resolvo /app/extraido/ ./
USER resolvo
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-Xshare:auto", "-jar", "app.jar"]
