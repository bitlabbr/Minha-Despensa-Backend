# Build stage
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /app

# Copia configurações Gradle para cache de dependências
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN ./gradlew dependencies --no-daemon || true

# Copia código-fonte e compila JAR executável
COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuário não-root para segurança
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/build/libs/*.jar app.jar

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
