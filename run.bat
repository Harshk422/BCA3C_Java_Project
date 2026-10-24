@echo off
cd /d "%~dp0"
if not exist out mkdir out
javac -d out src\*.java
if errorlevel 1 (
  echo Compilation failed. Is the JDK installed? Try: javac -version
  pause
  exit /b 1
)
java -cp out Main
