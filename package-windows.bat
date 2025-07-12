@echo off
REM Windows packaging script for ConvertISO20022
REM Creates a native Windows application and MSI installer

setlocal enabledelayedexpansion

set APP_NAME=ConvertISO20022
set APP_VERSION=1.0.0
set MAIN_CLASS=com.fintech.payments.PaymentConverterApp

echo Starting Windows packaging for %APP_NAME%...

REM Clean and build the project
echo Building project...
call mvn clean package
if %ERRORLEVEL% neq 0 (
    echo Build failed!
    exit /b 1
)

REM Create output directory
if not exist dist\windows mkdir dist\windows

REM Create Windows executable
echo Creating Windows executable...
jpackage ^
    --input target ^
    --name "%APP_NAME%" ^
    --main-jar "%APP_NAME%-%APP_VERSION%.jar" ^
    --main-class "%MAIN_CLASS%" ^
    --type app-image ^
    --dest dist\windows ^
    --app-version "%APP_VERSION%" ^
    --vendor "FinTech Payments" ^
    --description "Convert legacy payment formats to ISO 20022" ^
    --icon src\main\resources\icon.ico ^
    --java-options "-Xmx1024m" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --win-console

if %ERRORLEVEL% neq 0 (
    echo App image creation failed!
    exit /b 1
)

REM Create MSI installer
echo Creating MSI installer...
jpackage ^
    --input target ^
    --name "%APP_NAME%" ^
    --main-jar "%APP_NAME%-%APP_VERSION%.jar" ^
    --main-class "%MAIN_CLASS%" ^
    --type msi ^
    --dest dist\windows ^
    --app-version "%APP_VERSION%" ^
    --vendor "FinTech Payments" ^
    --description "Convert legacy payment formats to ISO 20022" ^
    --icon src\main\resources\icon.ico ^
    --java-options "-Xmx1024m" ^
    --java-options "-Dfile.encoding=UTF-8" ^
    --win-console ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut

if %ERRORLEVEL% neq 0 (
    echo MSI creation failed!
    exit /b 1
)

echo Windows packaging complete!
echo Executable: dist\windows\%APP_NAME%\%APP_NAME%.exe
echo MSI installer: dist\windows\%APP_NAME%-%APP_VERSION%.msi

endlocal