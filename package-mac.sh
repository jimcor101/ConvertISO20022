#!/bin/bash

# Mac packaging script for ConvertISO20022
# Creates a native macOS application bundle and DMG installer

set -e

APP_NAME="ConvertISO20022"
APP_VERSION="1.0.0"
MAIN_CLASS="com.fintech.payments.PaymentConverterApp"
MODULE_PATH=""

echo "Starting Mac packaging for $APP_NAME..."

# Clean and build the project
echo "Building project..."
mvn clean package

# Create runtime image with jpackage
echo "Creating macOS app bundle..."
jpackage \
    --input target \
    --name "$APP_NAME" \
    --main-jar "$APP_NAME-$APP_VERSION.jar" \
    --main-class "$MAIN_CLASS" \
    --type app-image \
    --dest dist/mac \
    --app-version "$APP_VERSION" \
    --vendor "FinTech Payments" \
    --description "Convert legacy payment formats to ISO 20022" \
    --icon src/main/resources/icon.icns \
    --java-options "-Xmx1024m" \
    --java-options "-Dfile.encoding=UTF-8"

# Create DMG installer
echo "Creating DMG installer..."
jpackage \
    --input target \
    --name "$APP_NAME" \
    --main-jar "$APP_NAME-$APP_VERSION.jar" \
    --main-class "$MAIN_CLASS" \
    --type dmg \
    --dest dist/mac \
    --app-version "$APP_VERSION" \
    --vendor "FinTech Payments" \
    --description "Convert legacy payment formats to ISO 20022" \
    --icon src/main/resources/icon.icns \
    --java-options "-Xmx1024m" \
    --java-options "-Dfile.encoding=UTF-8" \
    --mac-package-name "$APP_NAME" \
    --mac-package-identifier "com.fintech.payments.convertiso20022"

echo "Mac packaging complete!"
echo "App bundle: dist/mac/$APP_NAME.app"
echo "DMG installer: dist/mac/$APP_NAME-$APP_VERSION.dmg"

# Optional: Code signing (uncomment and configure if needed)
# echo "Code signing..."
# codesign --force --deep --sign "Developer ID Application: Your Name" "dist/mac/$APP_NAME.app"
# codesign --verify --verbose "dist/mac/$APP_NAME.app"