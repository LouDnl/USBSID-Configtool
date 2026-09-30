#!/usr/bin/env bash
# USBSID-Pico is a RPi Pico (RP2040/RP2350) based board for interfacing one
# or two MOS SID chips and/or hardware SID emulators over (WEB)USB with your
# computer, phone or ASID supporting player.
#
# install-libusb4java.sh (Configtool build script)
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
