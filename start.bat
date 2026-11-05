@echo off
chcp 65001 >nul
rem Запуск PrintMasterCRM на Windows двойным щелчком. Нужна Java 17 или новее (JDK).
cd /d "%~dp0"

call gradlew.bat bootJar -x test -q
if errorlevel 1 (
    echo Сборка не удалась. Проверьте, что установлена Java 17+.
    pause
    exit /b 1
)
for /f "delims=" %%i in ('dir /b /o-d build\libs\*.jar') do (
    set JAR=build\libs\%%i
    goto run
)
:run
echo PrintMasterCRM запускается. Браузер откроется сам: http://localhost:8765
echo База данных: %USERPROFILE%\PrintMasterCRM-data
java -Dprintmaster.open-browser=true -Dfile.encoding=UTF-8 -jar "%JAR%"
pause
