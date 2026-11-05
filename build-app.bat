@echo off
chcp 65001 >nul
rem Собирает самостоятельное приложение для Windows (со встроенной Java): dist\PrintMaster CRM\PrintMaster CRM.exe
rem Нужна JDK 17+ с jpackage (Temurin и т. п.). Собирать нужно на Windows — jpackage не умеет собирать для другой системы.
cd /d "%~dp0"

call gradlew.bat bootJar -x test -q
if errorlevel 1 ( echo Сборка не удалась & pause & exit /b 1 )
for /f "delims=" %%i in ('dir /b /o-d build\libs\*.jar') do (
    set JARNAME=%%i
    goto pack
)
:pack
if exist "dist\PrintMaster CRM" rmdir /s /q "dist\PrintMaster CRM"
jpackage --type app-image --name "PrintMaster CRM" --dest dist --input build\libs --main-jar "%JARNAME%" ^
  --main-class org.springframework.boot.loader.launch.JarLauncher ^
  --java-options "-Dprintmaster.open-browser=true -Dfile.encoding=UTF-8 -Xmx512m"
if errorlevel 1 ( echo jpackage не сработал. Нужна JDK 17+ ^(не JRE^), путь к ней должен быть в PATH & pause & exit /b 1 )

echo.
echo Готово: dist\PrintMaster CRM\PrintMaster CRM.exe
echo Папку "PrintMaster CRM" можно скопировать куда угодно и запускать .exe двойным щелчком. Закрывать — кнопкой "Выход" в программе.
echo Данные: %USERPROFILE%\PrintMasterCRM-data
explorer dist
pause
