# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Maven wrapper and pom first (cache dependencies)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src src
RUN ./mvnw package -DskipTests -B

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
