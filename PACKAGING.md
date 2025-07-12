# Packaging Instructions

This document describes how to create native installers for ConvertISO20022 on different platforms.

## Prerequisites

- Java 17+ with jpackage tool (included in JDK 14+)
- Maven 3.6+
- Platform-specific tools:
  - **macOS**: Xcode command line tools
  - **Windows**: WiX Toolset 3.11+ (for MSI creation)
  - **Linux**: rpm-build (for RPM packages)

## Required Icons

Before packaging, add platform-specific icons to `src/main/resources/`:
- `icon.icns` - macOS icon (512x512)
- `icon.ico` - Windows icon (multiple sizes)
- `icon.png` - Linux icon (512x512)

## Platform Scripts

### macOS
```bash
./package-mac.sh
```
Creates:
- `dist/mac/ConvertISO20022.app` - Application bundle
- `dist/mac/ConvertISO20022-1.0.0.dmg` - DMG installer

### Windows
```cmd
package-windows.bat
```
Creates:
- `dist/windows/ConvertISO20022/ConvertISO20022.exe` - Executable
- `dist/windows/ConvertISO20022-1.0.0.msi` - MSI installer

### Linux
```bash
./package-linux.sh
```
Creates:
- `dist/linux/convertiso20022_1.0.0-1_amd64.deb` - Debian package
- `dist/linux/convertiso20022-1.0.0-1.x86_64.rpm` - RPM package (if rpmbuild available)
- `dist/linux/ConvertISO20022/` - Portable app directory

## Installation

### macOS
1. Mount the DMG: `open ConvertISO20022-1.0.0.dmg`
2. Drag ConvertISO20022.app to Applications folder

### Windows
1. Run the MSI installer: `ConvertISO20022-1.0.0.msi`
2. Follow the installation wizard

### Linux

**Debian/Ubuntu:**
```bash
sudo dpkg -i convertiso20022_1.0.0-1_amd64.deb
sudo apt-get install -f  # Fix dependencies if needed
```

**RHEL/Fedora/CentOS:**
```bash
sudo rpm -i convertiso20022-1.0.0-1.x86_64.rpm
```

**Portable (any Linux):**
```bash
./dist/linux/ConvertISO20022/bin/ConvertISO20022
```

## Code Signing (Optional)

### macOS
Uncomment and configure the code signing section in `package-mac.sh`:
```bash
codesign --force --deep --sign "Developer ID Application: Your Name" "dist/mac/ConvertISO20022.app"
```

### Windows
Use signtool.exe with your code signing certificate:
```cmd
signtool sign /f certificate.p12 /p password dist\windows\ConvertISO20022-1.0.0.msi
```

## Troubleshooting

### Common Issues

1. **Missing jpackage**: Ensure you're using JDK 14+ or install jpackage separately
2. **Icon not found**: Add the required icon files to `src/main/resources/`
3. **WiX not found (Windows)**: Install WiX Toolset and add to PATH
4. **RPM build fails (Linux)**: Install rpm-build package

### Memory Configuration

The default memory allocation is 1GB (`-Xmx1024m`). Modify the `--java-options` in the scripts to adjust:
```bash
--java-options "-Xmx2048m"  # 2GB memory
```

### Debugging

To debug packaging issues, add verbose output:
```bash
jpackage --verbose [other options]
```