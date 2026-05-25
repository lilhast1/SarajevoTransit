# RSA private key must exist at src/main/resources/keys/rsa-private-key.pem before building.
# Run the keygen commands from the README first if this is a fresh clone.
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src ./src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
ENV JAVA_TOOL_OPTIONS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Xss256k -XX:MaxRAMPercentage=75.0"
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
