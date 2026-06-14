# ---- Build stage: compile and package the application with Maven on JDK 21 ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Cache dependencies first: only re-downloaded when the pom changes, not on every source edit.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime stage: slim JRE image that runs the fat jar ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as a non-root user.
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/target/restaurant-finder-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
