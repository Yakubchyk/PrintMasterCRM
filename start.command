#!/bin/bash
# Запуск PrintMasterCRM двойным щелчком (macOS). Нужна Java 17.
cd "$(dirname "$0")" || exit 1

JAR=$(ls build/libs/*.jar 2>/dev/null | head -n 1)
if [ -z "$JAR" ]; then
    echo "Сборка не найдена, собираю (первый раз это занимает несколько минут)..."
    ./gradlew bootJar -x test || { echo "Сборка не удалась"; read -r -p "Нажмите Enter..."; exit 1; }
    JAR=$(ls build/libs/*.jar | head -n 1)
fi

# Через несколько секунд открываем браузер
( sleep 6 && open "http://localhost:8080" ) &

echo "PrintMasterCRM запущен. Закройте это окно, чтобы остановить программу."
echo "База данных: ~/PrintMasterCRM-data"
java -jar "$JAR"
