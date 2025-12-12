@echo off
echo ============================================
echo Echofy Release Build Script
echo ============================================
echo.

cd /d "C:\Users\chenk\AndroidStudioProjects\OpenTune"

REM Check if keystore already exists
if exist "echofy-release-key.jks" (
    echo Keystore already exists. Proceeding to build...
    goto :build
)

echo.
echo Step 1: Creating Release Keystore
echo ==================================
echo.
echo IMPORTANT: Remember these details:
echo   - Password: Echofy2024
echo   - When asked for name/org, you can press Enter to skip
echo.

keytool -genkeypair -v -keystore echofy-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias echofy -storepass Echofy2024 -keypass Echofy2024 -dname "CN=Chenkham, OU=Echofy, O=Echofy, L=Unknown, ST=Unknown, C=IN"

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Failed to create keystore.
    echo Make sure Java JDK is installed and keytool is in PATH.
    echo.
    echo Try installing Java from: https://adoptium.net/
    pause
    exit /b 1
)

echo.
echo Keystore created successfully!
echo.

:build
echo.
echo Step 2: Cleaning and Building Release APK
echo ==========================================
echo.

REM Delete old configuration cache to avoid issues
rmdir /s /q ".gradle\configuration-cache" 2>nul

call gradlew.bat clean --no-configuration-cache
call gradlew.bat assembleRelease --no-configuration-cache

if %ERRORLEVEL% neq 0 (
    echo.
    echo ERROR: Build failed. Check the error messages above.
    pause
    exit /b 1
)

echo.
echo ============================================
echo BUILD SUCCESSFUL!
echo ============================================
echo.
echo Your signed APK files are located at:
echo.
dir /b "app\build\outputs\apk\release\*.apk" 2>nul
echo.
echo Full path: app\build\outputs\apk\release\
echo.
echo These APKs are properly signed and can be
echo shared with your friends!
echo.
pause

