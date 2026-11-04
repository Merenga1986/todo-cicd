# Стадия сборки
FROM maven:3.8.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
# Загрузка зависимостей отдельным слоем: кэшируется, пока не меняется pom.xml
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Сборка проекта (тесты уже прогнаны в CI на шаге build-and-test)
RUN mvn -B -q package -DskipTests

# Стадия выполнения
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# curl нужен для HEALTHCHECK; кэш apt чистим, чтобы не раздувать образ
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 1001 app

# Копирование JAR-файла из стадии builder
COPY --from=builder /app/target/todo.jar app.jar

USER app
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1

# Команда для запуска приложения
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
