FROM gradle:7.6.0-jdk17 AS build
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src/ ./src/
RUN gradle build -x test

FROM openjdk:17-jdk-slim AS run
COPY --from=build /app/build/libs/*.jar /app/app.jar
CMD ["java", "-jar", "/app/app.jar"]