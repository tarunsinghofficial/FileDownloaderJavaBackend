@echo off
echo Compiling Java files...

if not exist "bin" mkdir bin

javac -source 17 -target 17 -d bin -cp "lib/*" src/*.java

if %errorlevel% equ 0 (
    echo Compilation successful!
) else (
    echo Compilation failed!
    pause
    exit /b %errorlevel%
)