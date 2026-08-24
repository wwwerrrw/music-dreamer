FROM maven:3.9.9-eclipse-temurin-21 AS build

ARG SERVICE
WORKDIR /workspace

COPY backend ./backend
COPY database ./database

RUN test -n "$SERVICE" \
    && mvn -f backend/pom.xml -pl "$SERVICE" -am -DskipTests package

FROM eclipse-temurin:21-jre-jammy

ARG SERVICE
ENV SERVICE="$SERVICE"
WORKDIR /app

COPY --from=build /workspace/backend/${SERVICE}/target/${SERVICE}-0.1.0.jar /app/app.jar

RUN useradd --system --uid 10001 --create-home appuser \
    && mkdir -p /app/media \
    && chown -R appuser:appuser /app

USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
