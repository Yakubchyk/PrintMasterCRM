FROM gradle:7.6.0-jdk17 as build
WORKDIR /app
COPY pom.xml .

RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

COPY src/ ./src/

RUN mvn clean package -DskipTests=true

FROM openjdk:17-jdk-slim as run
RUN mkdir /app
COPY --from=build /app/target/*.jar /app/app.jar
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]