#!/bin/bash
# Собирает самостоятельное приложение «PrintMaster CRM.app» (со встроенной Java) — запускается двойным щелчком,
# без IntelliJ и без установленной Java. Результат: папка dist/. Запускать нужно один раз после каждого обновления программы.
cd "$(dirname "$0")" || exit 1

pause() { read -r -p "Нажмите Enter..."; }

JH=$(/usr/libexec/java_home -v 17+ 2>/dev/null)
if [ -z "$JH" ] || [ ! -x "$JH/bin/jpackage" ]; then
    echo "Не найдена Java (JDK) 17 или новее с jpackage. Её использует IntelliJ — установите JDK 17+ (например, Temurin)."
    pause; exit 1
fi
export JAVA_HOME="$JH"
APP="PrintMaster CRM"

echo "1/3 Собираю программу..."
./gradlew bootJar -x test -q || { echo "Сборка не удалась"; pause; exit 1; }
JAR=$(ls -t build/libs/*.jar | head -n 1)
JARNAME=$(basename "$JAR")

echo "2/3 Готовлю значок..."
ICONSET=build/app.iconset
rm -rf "$ICONSET" build/app.icns; mkdir -p "$ICONSET"
if [ -f packaging/icon.png ]; then
    for s in 16 32 64 128 256 512; do
        sips -z $s $s packaging/icon.png --out "$ICONSET/icon_${s}x${s}.png" >/dev/null
        sips -z $((s*2)) $((s*2)) packaging/icon.png --out "$ICONSET/icon_${s}x${s}@2x.png" >/dev/null
    done
    iconutil -c icns "$ICONSET" -o build/app.icns
fi
ICON_ARG=()
[ -f build/app.icns ] && ICON_ARG=(--icon build/app.icns)

echo "3/3 Собираю приложение (1–2 минуты)..."
rm -rf "dist/$APP.app"; mkdir -p dist
"$JH/bin/jpackage" --type app-image --name "$APP" --dest dist \
    --input build/libs --main-jar "$JARNAME" \
    --main-class org.springframework.boot.loader.launch.JarLauncher \
    --runtime-image "$JH" "${ICON_ARG[@]}" \
    --java-options "-Dprintmaster.open-browser=true -Dfile.encoding=UTF-8 -Xmx512m" \
    || { echo "jpackage не сработал"; pause; exit 1; }

# Без значка в Dock: программа работает в фоне, значок не прыгает. Остановка — кнопка «Выход» в меню программы.
PLIST="dist/$APP.app/Contents/Info.plist"
/usr/libexec/PlistBuddy -c "Delete :LSUIElement" "$PLIST" >/dev/null 2>&1
/usr/libexec/PlistBuddy -c "Add :LSUIElement bool true" "$PLIST" \
    && codesign --force --deep --sign - "dist/$APP.app" >/dev/null 2>&1

echo
echo "Готово: dist/$APP.app"
echo "Значка в Dock у программы нет (чтобы не прыгал) — открывайте http://localhost:8765 (добавьте в закладки), закрывайте кнопкой «Выход»."
echo "Перетащите его в «Программы» (Applications) и запускайте двойным щелчком. Закрывать — кнопкой «Выход» в программе."
echo "Если macOS скажет, что приложение от неизвестного разработчика: правый клик → «Открыть» (один раз)."
echo "Данные лежат в ~/PrintMasterCRM-data и не пропадают при обновлении приложения. Журнал: ~/PrintMasterCRM-data/printmaster.log"
open dist
pause
