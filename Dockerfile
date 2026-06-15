# ---- build stage ----
# Use the Maven image so the build works even without a local JDK/Maven.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache dependencies in their own layer (incl. the JitPack chesslib fetch).
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- runtime stage ----
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
USER app
COPY --from=build /app/target/games-service-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
