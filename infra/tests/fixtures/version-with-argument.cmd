@echo off
if "%~1"=="--version" (
    echo fixture 1.2.3
    exit /b 0
)
exit /b 9
