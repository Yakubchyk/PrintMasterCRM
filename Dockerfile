# Используем базовый образ Gradle с JDK 17
FROM gradle:7.6.0-jdk17 AS build

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем Gradle файлы и настройки
COPY build.gradle settings.gradle ./

# Копируем исходный код
COPY src/ ./src/

# Собираем проект
RUN gradle build -x test

# Переходим на облегченный образ для выполнения
FROM openjdk:17-jdk-slim AS run

# Копируем скомпилированный JAR из этапа сборки
COPY --from=build /app/build/libs/*.jar /app/app.jar

# Устанавливаем команду запуска
CMD ["java", "-jar", "/app/app.jar"]