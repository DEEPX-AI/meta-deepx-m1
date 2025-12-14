SUMMARY = "Library for YUV conversion and scaling"
DESCRIPTION = "libyuv is an open source project that includes YUV conversion and scaling functionality."
HOMEPAGE = "https://chromium.googlesource.com/libyuv/libyuv/"
LICENSE = "BSD-3-Clause"
# NOTE: Please verify the checksum. If bitbake fails, it will provide the correct md5/sha256 values.
LIC_FILES_CHKSUM = "file://LICENSE;md5=464282cfb405b005b9637f11103a7325"

inherit cmake

SRC_URI = "git://chromium.googlesource.com/libyuv/libyuv;protocol=https;branch=main"
SRCREV = "a6a2ec654b1be1166b376476a7555c89eca0c275"

S = "${WORKDIR}/git"

DEPENDS = "libjpeg-turbo"

# CMake Configuration
EXTRA_OECMAKE += "-DBUILD_SHARED_LIBS=ON"
EXTRA_OECMAKE += "-DTEST=OFF"

# [FIX] Solve 'dev-elf' QA Issue
# libyuv creates a shared library 'libyuv.so' without version numbers (e.g., libyuv.so.1).
# Yocto puts '.so' files into the -dev package by default, assuming they are symlinks.
# Since this is a real binary, we must force it into the main package.

# 1. Clear FILES_SOLIBSDEV (Default is "${libdir}/lib*.so") to prevent putting .so in -dev.
FILES_SOLIBSDEV = ""

# 2. Add the .so file explicitly to the main package files.
FILES:${PN} += "${libdir}/libyuv.so"

# 3. Ensure headers go to the dev package (Standard behavior, but good to be explicit).
FILES:${PN}-dev += "${includedir}"