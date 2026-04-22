@echo off
cd /d C:\airProject\air-alert

echo ==== %date% %time% ==== >> scheduler.log
echo current dir: %cd% >> scheduler.log

"C:\Program Files\Zulu\zulu-8\bin\java.exe" -jar "C:\airProject\air-alert\air-alert.jar" >> scheduler.log 2>&1

echo exit code: %errorlevel% >> scheduler.log
echo. >> scheduler.log