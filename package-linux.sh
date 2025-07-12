#!/bin/bash

# Linux packaging script for ConvertISO20022
# Creates native Linux packages (DEB and RPM)

set -e

APP_NAME="ConvertISO20022"
APP_VERSION="1.0.0"
MAIN_CLASS="com.fintech.payments.PaymentConverterApp"

echo "Starting Linux packaging for $APP_NAME..."

# Clean and build the project
echo "Building project..."
mvn clean package

# Create output directory
mkdir -p dist/linux

# Create DEB package
echo "Creating DEB package..."
jpackage \
    --input target \
    --name "$APP_NAME" \
    --main-jar "$APP_NAME-$APP_VERSION.jar" \
    --main-class "$MAIN_CLASS" \
    --type deb \
    --dest dist/linux \
    --app-version "$APP_VERSION" \
    --vendor "FinTech Payments" \
    --description "Convert legacy payment formats to ISO 20022" \
    --icon src/main/resources/icon.png \
    --java-options "-Xmx1024m" \
    --java-options "-Dfile.encoding=UTF-8" \
    --linux-package-name "convertiso20022" \
    --linux-app-category "Office" \
    --linux-menu-group "Office" \
    --linux-shortcut

# Create RPM package (if rpmbuild is available)
if command -v rpmbuild &> /dev/null; then
    echo "Creating RPM package..."
    jpackage \
        --input target \
        --name "$APP_NAME" \
        --main-jar "$APP_NAME-$APP_VERSION.jar" \
        --main-class "$MAIN_CLASS" \
        --type rpm \
        --dest dist/linux \
        --app-version "$APP_VERSION" \
        --vendor "FinTech Payments" \
        --description "Convert legacy payment formats to ISO 20022" \
        --icon src/main/resources/icon.png \
        --java-options "-Xmx1024m" \
        --java-options "-Dfile.encoding=UTF-8" \
        --linux-package-name "convertiso20022" \
        --linux-app-category "Office" \
        --linux-menu-group "Office" \
        --linux-shortcut
else
    echo "Warning: rpmbuild not found. Skipping RPM package creation."
    echo "To create RPM packages, install rpm-build: sudo apt-get install rpm (Ubuntu/Debian) or dnf install rpm-build (Fedora/RHEL)"
fi

# Create AppImage (alternative portable format)
echo "Creating AppImage..."
jpackage \
    --input target \
    --name "$APP_NAME" \
    --main-jar "$APP_NAME-$APP_VERSION.jar" \
    --main-class "$MAIN_CLASS" \
    --type app-image \
    --dest dist/linux \
    --app-version "$APP_VERSION" \
    --vendor "FinTech Payments" \
    --description "Convert legacy payment formats to ISO 20022" \
    --icon src/main/resources/icon.png \
    --java-options "-Xmx1024m" \
    --java-options "-Dfile.encoding=UTF-8"

echo "Linux packaging complete!"
echo "DEB package: dist/linux/convertiso20022_$APP_VERSION-1_amd64.deb"
if command -v rpmbuild &> /dev/null; then
    echo "RPM package: dist/linux/convertiso20022-$APP_VERSION-1.x86_64.rpm"
fi
echo "App directory: dist/linux/$APP_NAME/"

echo ""
echo "Installation commands:"
echo "DEB: sudo dpkg -i dist/linux/convertiso20022_$APP_VERSION-1_amd64.deb"
if command -v rpmbuild &> /dev/null; then
    echo "RPM: sudo rpm -i dist/linux/convertiso20022-$APP_VERSION-1.x86_64.rpm"
fi