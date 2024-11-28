FROM eclipse-temurin:17-jdk-alpine

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем исходный код проекта в контейнер
COPY . .

# Выполняем сборку проекта внутри контейнера
RUN ./gradlew clean build --no-daemon

# Копируем скомпилированный JAR файл
COPY build/libs/printmastercrm-0.0.1-SNAPSHOT.jar app.jar

# Открываем порт 8080
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]