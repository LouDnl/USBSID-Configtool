#!/usr/bin/env bash
# Build a Linux AppImage from the jpackage app-image output.
#
# Prerequisites:
#  - Run 'clj -T:build package' first (creates target/package/USBSID-Pico-Configtool/)
#  - curl must be available
#  - appimagetool is auto-downloaded if absent
#
# Usage: ./scripts/build-appimage.sh [output-dir]
#  output-dir defaults to target/appimage/

set -euo pipefail

APP_NAME="USBSID-Pico-Configtool"
VERSION=$(cat resources/.version | tr -d '[:space:]')
ARCH=$(uname -m)   # x86_64 or aarch64

JPACKAGE_SRC="target/package/${APP_NAME}"
WORK_DIR="target/appimage-work"
APP_DIR="${WORK_DIR}/${APP_NAME}.AppDir"
OUTPUT_DIR="${1:-target/appimage}"
APPIMAGE_OUT="${OUTPUT_DIR}/${APP_NAME}-${VERSION}-${ARCH}.AppImage"
TOOL_CACHE="target/appimagetool-${ARCH}.AppImage"

# Verify jpackage output exists

if [ ! -d "$JPACKAGE_SRC" ]; then
    echo "ERROR: jpackage app-image not found at '${JPACKAGE_SRC}'"
    echo "       Run 'clj -T:build package' first."
    exit 1
fi

# Build AppDir

echo "Building AppDir from ${JPACKAGE_SRC}..."
rm -rf "$APP_DIR"
mkdir -p "$APP_DIR"

# Copy the entire jpackage app-image into AppDir root
cp -r "${JPACKAGE_SRC}/." "$APP_DIR/"

# AppRun - delegates to the jpackage native launcher
cat > "${APP_DIR}/AppRun" << 'APPRUN'
#!/bin/sh
SELF_DIR="$(dirname "$(readlink -f "$0")")"
exec "${SELF_DIR}/bin/USBSID-Pico-Configtool" "$@"
APPRUN
chmod +x "${APP_DIR}/AppRun"

# .desktop entry (required by AppImage spec)
cat > "${APP_DIR}/${APP_NAME}.desktop" << DESKTOP
[Desktop Entry]
Name=USBSID-Pico Configtool
Comment=Configuration tool for USBSID-Pico boards
Exec=USBSID-Pico-Configtool
Icon=USBSID-Pico-Configtool
Type=Application
Categories=Utility;Electronics;
DESKTOP

# Icon - use project icon, fall back to 1×1 placeholder
ICON_CANDIDATES=(
    "resources/usbsid-configtool-icon.png"
    "resources/usbsid-configtool-logo.png"
    "resources/usbsid-logo.png"
    "resources/icon.png"
)
ICON_SRC=""
for c in "${ICON_CANDIDATES[@]}"; do
    if [ -f "$c" ]; then
        ICON_SRC="$c"
        break
    fi
done

if [ -n "$ICON_SRC" ]; then
    cp "$ICON_SRC" "${APP_DIR}/${APP_NAME}.png"
    echo "  Icon: ${ICON_SRC}"
else
    # Minimal 1×1 transparent PNG as placeholder
    printf '\x89PNG\r\n\x1a\n\x00\x00\x00\rIHDR\x00\x00\x00\x01\x00\x00\x00\x01\x08\x06\x00\x00\x00\x1f\x15\xc4\x89\x00\x00\x00\nIDATx\x9cc\x00\x01\x00\x00\x05\x00\x01\r\n-\xb4\x00\x00\x00\x00IEND\xaeB`\x82' \
        > "${APP_DIR}/${APP_NAME}.png"
    echo "  WARN: no icon found - using 1×1 placeholder"
fi

# Download appimagetool if needed

if [ ! -x "$TOOL_CACHE" ]; then
    TOOL_URL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${ARCH}.AppImage"
    echo "Downloading appimagetool for ${ARCH}..."
    curl -fsSL -o "$TOOL_CACHE" "$TOOL_URL"
    chmod +x "$TOOL_CACHE"
fi

# Create AppImage

mkdir -p "$OUTPUT_DIR"

echo "Creating AppImage..."
# APPIMAGE_EXTRACT_AND_RUN=1 avoids needing FUSE (works on all systems incl. CI)
APPIMAGE_EXTRACT_AND_RUN=1 ARCH="$ARCH" \
    "$TOOL_CACHE" --no-appstream "$APP_DIR" "$APPIMAGE_OUT"

chmod +x "$APPIMAGE_OUT"
echo "Built: ${APPIMAGE_OUT} ($(du -sh "$APPIMAGE_OUT" | cut -f1))"
