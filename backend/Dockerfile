# ---- Build stage: compile the jar with Maven ----
FROM maven:3.9-eclipse-temurin-24 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Run stage: small JRE image ----
FROM eclipse-temurin:24-jre
WORKDIR /app
COPY --from=build /app/target/splitsmart-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
