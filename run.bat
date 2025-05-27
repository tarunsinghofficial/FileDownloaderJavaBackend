@echo off
echo Starting File Downloader Server...

java -cp "bin;lib/*" FileDownloaderServer

if %errorlevel% neq 0 (
    echo Server failed to start!
    pause
    exit /b %errorlevel%
)