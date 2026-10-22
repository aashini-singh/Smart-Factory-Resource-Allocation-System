@echo off
REM Compile and run the Smart Factory Resource Allocation System (Windows)
cd /d "%~dp0"
if exist out rmdir /s /q out
javac -d out -sourcepath src src\smartfactory\main\FactoryApp.java
if errorlevel 1 (
    echo Compilation failed.
    pause
    exit /b 1
)
java -cp out smartfactory.main.FactoryApp
pause
