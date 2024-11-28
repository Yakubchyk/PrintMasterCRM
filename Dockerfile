FROM gradle:7.6.0-jdk17 AS build
WORKDIR /app
COPY build.gradle .
#RUN mvn dependency:go-offline
COPY src/ ./src/
RUN mvn clean package -DskipTests=true

FROM openjdk:17-jdk-slim as run
RUN mkdir /app
COPY --from=build /app/target/*.jar /app/app.jar
WORKDIR /app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]