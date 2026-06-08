#!/usr/bin/env bash
# Override the stock org.usb4java:libusb4java:1.3.0 win32 native-lib jars in the
# local Maven repo with patched ones that bundle a modern libusb core.
#
# Why: the win32-x86-64/win32-x86 jars on Maven Central bundle a statically-linked
# libusb core compiled ~Oct 2018 (pre libusb 1.0.24). That core lacks
# WinUsb_GetAssociatedInterface support, so claiming the second interface of an
# IAD-grouped composite USB function (our CDC control+data pair) fails with
# LIBUSB_ERROR_NOT_SUPPORTED (-12) on Windows, even though native C apps linking
# a modern system libusb (1.0.26/1.0.27) claim it fine.
#
# The patched jars in assets/lib/ contain a rebuilt libusb4java.dll linked
# against a modern libusb core. This script drops them into ~/.m2 with the exact
# artifact filenames Maven/tools.deps expect, so they're picked up instead of the
# stock Central copies.
#
# Usage: ./scripts/install-libusb4java.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SRC_DIR="${REPO_ROOT}/assets/lib"
DEST_DIR="${HOME}/.m2/repository/org/usb4java/libusb4java/1.3.0"

JARS=(
  "libusb4java-1.3.0-win32-x86-64.jar"
  "libusb4java-1.3.0-win32-x86.jar"
)

mkdir -p "$DEST_DIR"

for jar in "${JARS[@]}"; do
  src="${SRC_DIR}/${jar}"
  if [ ! -f "$src" ]; then
    echo "ERROR: patched jar not found: ${src}" >&2
    exit 1
  fi

  dest="${DEST_DIR}/${jar}"
  cp "$src" "$dest"

  if command -v sha1sum >/dev/null 2>&1; then
    sha1sum "$dest" | cut -d' ' -f1 > "${dest}.sha1"
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 1 "$dest" | cut -d' ' -f1 > "${dest}.sha1"
  fi

  echo "Installed patched ${jar} -> ${dest} ($(wc -c < "$dest") bytes)"
done

echo "Done. Patched libusb4java native libs installed into ${DEST_DIR}"
