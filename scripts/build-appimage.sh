#!/usr/bin/env bash
# USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
# or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
# computer, phone or ASID supporting player.
#
# build-appimage.sh (Configtool build script)
# This file is part of USBSID-Configtool (https://github.com/LouDnl/USBSID-Configtool)
# File author: LouD
#
# USBSID-Configtool
# Copyright (C) 2024-2026  LouD
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#
# Additional permission under GNU GPL version 3 section 7
#
# If you modify this Program, or any covered work, by linking or combining it
# with Clojure or any other library licensed under the Eclipse Public License
# 1.0 or 2.0 (or a modified version of such a library), containing parts
# covered by the terms of the Eclipse Public License, the licensors of this
# Program grant you additional permission to convey the resulting work.
# Corresponding Source for a non-source form of such a combination shall
# include the source code for the parts of those libraries used as well as
# that of the covered work.

# Build a Linux AppImage from the jpackage app-image output.
#
# Prerequisites:
#  - Run 'clj -T:build package' first (creates target/package/USBSID-Configtool/)
#  - curl, zip and unzip must be available
#  - appimagetool is auto-downloaded if absent
#  - appstreamcli on PATH makes appimagetool validate the AppStream metainfo
#
# Optional environment:
#  APPIMAGE_UPDATE_INFO      update information to embed, also writes a .zsync file
#  APPIMAGE_SIGN_KEY         fingerprint of the gpg secret key to sign with
#  APPIMAGE_SIGN_PASSPHRASE  passphrase of that key
#  APPIMAGE_SIGN_PRIVATE_KEY secret key to import first (CI): armored export, or
#                            base64 of the binary export (maskable on GitLab)
#  JPACKAGE_SRC              jpackage app-image dir (default target/package/USBSID-Configtool)
#
# Usage: ./scripts/build-appimage.sh [output-dir]
#  output-dir defaults to target/appimage/

set -euo pipefail

APP_NAME="USBSID-Configtool"
APP_ID="nl.loudai.usbsid.configtool"
VERSION=$(cat resources/.version | tr -d '[:space:]')
ARCH=$(uname -m)   # x86_64 or aarch64

JPACKAGE_SRC="${JPACKAGE_SRC:-target/package/${APP_NAME}}"
META_SRC="resources/appimage"
OUTPUT_DIR="${1:-target/appimage}"
APPIMAGE_OUT="${OUTPUT_DIR}/${APP_NAME}-${VERSION}-${ARCH}.AppImage"
TOOL_CACHE="target/appimagetool-${ARCH}.AppImage"

# Verify jpackage output exists

if [ ! -d "$JPACKAGE_SRC" ]; then
    echo "ERROR: jpackage app-image not found at '${JPACKAGE_SRC}'"
    echo "       Run 'clj -T:build package' first."
    exit 1
fi

# Build AppDir in a fresh work dir, earlier work dirs are left untouched

WORK_BASE="${RUNNER_TEMP:-target}"
mkdir -p "$WORK_BASE"
WORK_DIR=$(mktemp -d "${WORK_BASE}/appimage-work.XXXXXX")
APP_DIR="${WORK_DIR}/${APP_NAME}.AppDir"
mkdir -p "$APP_DIR"

echo "Building AppDir from ${JPACKAGE_SRC} in ${APP_DIR}..."

# Copy the entire jpackage app-image into AppDir root
cp -r "${JPACKAGE_SRC}/." "$APP_DIR/"

# Drop native libs the Linux AppImage never loads from the uberjar copy:
# Windows/macOS binaries, JavaFX WebView/media (unused by the app),
# usb4java natives for other CPU archs
case "$ARCH" in
    x86_64) USB4JAVA_KEEP="linux-x86-64" ;;
    *)      USB4JAVA_KEEP="linux-${ARCH}" ;;
esac
for jar in "${APP_DIR}"/lib/app/*.jar; do
    size_before=$(du -k "$jar" | cut -f1)
    mapfile -t drop_entries < <(unzip -Z1 "$jar" \
        | grep -E '\.(dll|dylib|jnilib)$|(^|/)lib(jfxwebkit|jfxmedia|gstreamer-lite|fxplugins|avplugin[^/]*)\.so$|^org/usb4java/(linux|win32|darwin)-[^/]+/' \
        | grep -v "^org/usb4java/${USB4JAVA_KEEP}/" || true)
    if [ "${#drop_entries[@]}" -gt 0 ]; then
        zip -q -d "$jar" "${drop_entries[@]}"
    fi
    echo "  Trimmed $(basename "$jar"): ${#drop_entries[@]} entries, ${size_before}K -> $(du -k "$jar" | cut -f1)K"
done

# AppRun - delegates to the jpackage native launcher
cat > "${APP_DIR}/AppRun" << 'APPRUN'
#!/bin/sh
SELF_DIR="$(dirname "$(readlink -f "$0")")"
exec "${SELF_DIR}/bin/USBSID-Configtool" "$@"
APPRUN
chmod +x "${APP_DIR}/AppRun"

# Desktop entry, root copy is a symlink (AppImage spec needs one at the root)
DESKTOP_FILE="${APP_DIR}/usr/share/applications/${APP_ID}.desktop"
install -Dm644 "${META_SRC}/${APP_ID}.desktop" "$DESKTOP_FILE"
printf 'X-AppImage-Version=%s\n' "$VERSION" >> "$DESKTOP_FILE"
ln -s "usr/share/applications/${APP_ID}.desktop" "${APP_DIR}/${APP_ID}.desktop"

# Icons: hicolor theme sizes plus the AppDir root icon
for size in 256 512; do
    install -Dm644 "${META_SRC}/usbsid-configtool-icon-${size}.png" \
        "${APP_DIR}/usr/share/icons/hicolor/${size}x${size}/apps/${APP_ID}.png"
done
ln -s "usr/share/icons/hicolor/256x256/apps/${APP_ID}.png" "${APP_DIR}/${APP_ID}.png"

# AppStream metainfo, <releases> generated from CHANGELOG.md
#  - newest 10 "#### Version: x.y.z" headings, "* " bullets become the description
#  - date: tag v<version>, else the commit adding the heading, else today
#  - resources/.version is added when CHANGELOG.md has no heading for it yet

RELEASES_FILE="${WORK_DIR}/releases.xml"
awk -v max=10 -v current="$VERSION" -v today="$(date -u +%F)" '
function esc(s) {
    gsub(/&/, "\\&amp;", s); gsub(/</, "\\&lt;", s); gsub(/>/, "\\&gt;", s); gsub(/`/, "", s)
    return s
}
function reldate(v,    d, cmd) {
    d = ""
    cmd = "git log -1 --format=%as refs/tags/v" v " 2>/dev/null"
    cmd | getline d; close(cmd)
    if (d == "") {
        cmd = "git log -1 --format=%as -S\"#### Version: " v "\" -- CHANGELOG.md 2>/dev/null"
        cmd | getline d; close(cmd)
    }
    if (d == "") d = today
    return d
}
function flush() {
    if (ver == "") return
    if (n < max) {
        out = out sprintf("    <release version=\"%s\" date=\"%s\">", ver, reldate(ver))
        if (items != "") out = out "<description><ul>" items "</ul></description>"
        out = out "</release>\n"
        n++
    }
    ver = ""; items = ""
}
/^#### Version:/ { flush(); ver = $3; if (ver == current) seen = 1; next }
/^\* / && ver != "" { sub(/^\* /, ""); items = items "<li>" esc($0) "</li>"; next }
END {
    flush()
    if (!seen) out = sprintf("    <release version=\"%s\" date=\"%s\"/>\n", current, today) out
    printf "%s", out
}' CHANGELOG.md > "$RELEASES_FILE"

# appimagetool only validates the .appdata.xml name
METAINFO="${APP_DIR}/usr/share/metainfo/${APP_ID}.appdata.xml"
mkdir -p "$(dirname "$METAINFO")"
sed -e "/^[[:space:]]*@RELEASES@[[:space:]]*$/{r ${RELEASES_FILE}" -e 'd}' "${META_SRC}/${APP_ID}.appdata.xml" > "$METAINFO"

# Download appimagetool if needed

if [ ! -x "$TOOL_CACHE" ]; then
    mkdir -p "$(dirname "$TOOL_CACHE")"
    TOOL_URL="https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-${ARCH}.AppImage"
    echo "Downloading appimagetool for ${ARCH}..."
    curl -fsSL -o "$TOOL_CACHE" "$TOOL_URL"
    chmod +x "$TOOL_CACHE"
fi

# Update information and signing, both optional

if [ -n "${APPIMAGE_SIGN_PRIVATE_KEY:-}" ]; then
    echo "  Importing signing key"
    case "$APPIMAGE_SIGN_PRIVATE_KEY" in
        -----BEGIN*) printf '%s\n' "$APPIMAGE_SIGN_PRIVATE_KEY" | gpg --batch --import ;;
        *)           printf '%s' "$APPIMAGE_SIGN_PRIVATE_KEY" | base64 -d | gpg --batch --import ;;
    esac
fi
if [ -n "${APPIMAGE_SIGN_KEY:-}" ] && ! gpg --batch --list-secret-keys "$APPIMAGE_SIGN_KEY" >/dev/null 2>&1; then
    echo "  WARN: secret key ${APPIMAGE_SIGN_KEY} not in keyring, AppImage stays unsigned"
    APPIMAGE_SIGN_KEY=""
fi

TOOL_ARGS=()
if [ -n "${APPIMAGE_UPDATE_INFO:-}" ]; then
    echo "  Update information: ${APPIMAGE_UPDATE_INFO}"
    TOOL_ARGS+=(--updateinformation "$APPIMAGE_UPDATE_INFO")
fi
if [ -n "${APPIMAGE_SIGN_KEY:-}" ]; then
    echo "  Signing with key: ${APPIMAGE_SIGN_KEY}"
    TOOL_ARGS+=(--sign --sign-key "$APPIMAGE_SIGN_KEY")
    if [ -n "${APPIMAGE_SIGN_PASSPHRASE:-}" ]; then
        export APPIMAGETOOL_SIGN_PASSPHRASE="$APPIMAGE_SIGN_PASSPHRASE"
    fi
fi

# Create AppImage

mkdir -p "$OUTPUT_DIR"

echo "Creating AppImage..."
# Run from OUTPUT_DIR, appimagetool writes the .zsync into its working dir
TOOL_ABS=$(readlink -f "$TOOL_CACHE")
APP_DIR_ABS=$(readlink -f "$APP_DIR")
# APPIMAGE_EXTRACT_AND_RUN=1 avoids needing FUSE (works on all systems incl. CI)
(
    cd "$OUTPUT_DIR"
    APPIMAGE_EXTRACT_AND_RUN=1 ARCH="$ARCH" \
        "$TOOL_ABS" ${TOOL_ARGS[@]+"${TOOL_ARGS[@]}"} "$APP_DIR_ABS" "$(basename "$APPIMAGE_OUT")"
)

chmod +x "$APPIMAGE_OUT"
echo "Built: ${APPIMAGE_OUT} ($(du -sh "$APPIMAGE_OUT" | cut -f1))"
if [ -f "${APPIMAGE_OUT}.zsync" ]; then
    echo "Built: ${APPIMAGE_OUT}.zsync"
elif [ -n "${APPIMAGE_UPDATE_INFO:-}" ]; then
    echo "ERROR: update information set but no ${APPIMAGE_OUT}.zsync was written"
    exit 1
fi
