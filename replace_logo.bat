@echo off
echo ============================================
echo Echofy Logo Replacement Script
echo ============================================
echo.

REM Navigate to project directory
cd /d "C:\Users\chenk\AndroidStudioProjects\OpenTune"

REM Delete old vector files
echo Deleting old vector logo files...
del /f "app\src\main\res\drawable\echofy.xml" 2>nul
del /f "app\src\main\res\drawable\opentune.xml" 2>nul

REM Copy new PNG logo (using icon_logo.png - no background)
echo Copying new PNG logo (icon_logo.png)...
copy /y "app\src\main\icon_logo.png" "app\src\main\res\drawable\echofy.png"

echo.
echo ============================================
echo Logo replacement complete!
echo ============================================
echo.
echo Your new logo (icon_logo.png) has been copied as echofy.png
echo.
echo Now rebuild your project in Android Studio:
echo   Build -^> Clean Project
echo   Build -^> Rebuild Project
echo.
pause

