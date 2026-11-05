#!/bin/bash
# Запуск PrintMasterCRM двойным щелчком (macOS). Нужна Java 17 или новее.
# Собирает программу (если были изменения), запускает и открывает браузер. Закройте окно или нажмите «Выход» в программе, чтобы остановить.
cd "$(dirname "$0")" || exit 1

pause() { read -r -p "Нажмите Enter..."; }

JH=$(/usr/libexec/java_home -v 17+ 2>/dev/null)
[ -n "$JH" ] && export JAVA_HOME="$JH"
JAVA=${JAVA_HOME:+$JAVA_HOME/bin/}java

echo "Проверяю сборку (первый раз — несколько минут)..."
./gradlew bootJar -x test -q || { echo "Сборка не удалась"; pause; exit 1; }
JAR=$(ls -t build/libs/*.jar | head -n 1)

echo "PrintMasterCRM запускается. Браузер откроется сам: http://localhost:8765"
echo "База данных: ~/PrintMasterCRM-data"
"$JAVA" -Dprintmaster.open-browser=true -jar "$JAR"
