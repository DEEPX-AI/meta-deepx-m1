SUMMARY = "ONNX Runtime prebuilt binaries"
DESCRIPTION = "Microsoft ONNX Runtime prebuilt libraries and headers for inference (precompiled import)."
HOMEPAGE = "https://onnxruntime.ai/"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"


ONNX_VERSION = "1.20.1"
ONNX_ARCH = "aarch64"
ONNX_PACKAGE_NAME = "onnxruntime-linux-${ONNX_ARCH}-${ONNX_VERSION}.tgz"
ONNX_PACKAGE_FOLDER = "onnxruntime-linux-${ONNX_ARCH}-${ONNX_VERSION}"

#LIC_FILES_CHKSUM = "file://${ONNX_PACKAGE_FOLDER}/LICENSE;md5=0f7e3b1308cb5c00b372a6e78835732d"

# Download prebuilt ONNX Runtime from GitHub releases
SRC_URI = "https://github.com/microsoft/onnxruntime/releases/download/v${ONNX_VERSION}/${ONNX_PACKAGE_NAME};name=onnxruntime"

# SHA256 checksum for selected architecture
SRC_URI[onnxruntime.sha256sum] = "ae4fedbdc8c18d688c01306b4b50c63de3445cdf2dbd720e01a2fa3810b8106a"

S = "${WORKDIR}"

# Only compatible with aarch64
COMPATIBLE_MACHINE:aarch64 = "(.*)"

# Runtime dependencies
RDEPENDS:${PN} = "libstdc++"

# Disable configure and compile steps (prebuilt binaries)
do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    bbnote "Starting ONNX Runtime installation"
   
    # Create destination directories in rootfs (default prefix=/usr; set prefix="/usr/local" if desired earlier)
    install -d ${D}${prefix}/lib
    install -d ${D}${prefix}/include
    
    # Copy all contents to /usr/local but avoid preserving host uid/gid to prevent host contamination
    bbnote "Installing ONNX Runtime to ${D}${prefix} (rootfs /usr/local)"
    cp -R --preserve=mode,timestamps ${ONNX_PACKAGE_FOLDER}/* ${D}${prefix}/
    
    # Set proper permissions
    find ${D}${prefix} -type f -name "*.so*" -exec chmod 755 {} \;
    find ${D}${prefix} -type f -name "*.h" -exec chmod 644 {} \;
    find ${D}${prefix} -type f -name "*.hpp" -exec chmod 644 {} \;
    find ${D}${prefix} -type d -exec chmod 755 {} \;
    
    bbnote "ONNX Runtime installation completed"
    
    # Log installed files
    if [ -d "${D}${prefix}/lib" ]; then
        bbnote "Installed libraries:"
        ls -la ${D}${prefix}/lib/ | head -5
    fi

    # Relocate documentation / metadata files to standard locations to ensure they are packaged
    install -d ${D}${docdir}/${PN}
    for f in README.md ThirdPartyNotices.txt Privacy.md VERSION_NUMBER GIT_COMMIT_ID; do
        if [ -f ${D}${prefix}/$f ]; then
            mv ${D}${prefix}/$f ${D}${docdir}/${PN}/
        fi
    done

    # Install license file (if present) into licenses directory used by Yocto
    if [ -f ${ONNX_PACKAGE_FOLDER}/LICENSE ]; then
        install -d ${D}${datadir}/licenses/${PN}
        install -m 0644 ${ONNX_PACKAGE_FOLDER}/LICENSE ${D}${datadir}/licenses/${PN}/LICENSE
        if [ -f ${D}${prefix}/LICENSE ]; then
            rm -f ${D}${prefix}/LICENSE
        fi
    fi

    # Generate pkg-config if missing in archive
    if [ ! -f ${D}${prefix}/lib/pkgconfig/onnxruntime.pc ]; then
        install -d ${D}${prefix}/lib/pkgconfig
        cat > ${D}${prefix}/lib/pkgconfig/onnxruntime.pc <<EOF
prefix=${prefix}
exec_prefix=\${prefix}
libdir=\${exec_prefix}/lib
includedir=\${prefix}/include

Name: ONNXRuntime
Description: Microsoft ONNX Runtime (prebuilt)
Version: ${ONNX_VERSION}
Libs: -L\${libdir} -lonnxruntime
Cflags: -I\${includedir}
EOF
    fi

    # Normalize ownership to root:root (some archives may carry non-root ids)
    chown -R 0:0 ${D}${prefix} || true
    chown -R 0:0 ${D}${docdir}/${PN} || true
    if [ -d ${D}${datadir}/licenses/${PN} ]; then
        chown -R 0:0 ${D}${datadir}/licenses/${PN}
    fi
}

########################################
# Packaging
########################################
PACKAGES = "${PN} ${PN}-dev ${PN}-doc"

# Runtime libs & executables
FILES:${PN} = " \
    ${prefix}/lib/libonnxruntime.so.* \
    ${prefix}/lib/libonnxruntime_providers_*.so* \
    ${prefix}/bin/* \
"

# Development headers & symlinks
FILES:${PN}-dev = " \
    ${prefix}/include/* \
    ${prefix}/lib/libonnxruntime.so \
    ${prefix}/lib/*.a \
    ${prefix}/lib/pkgconfig/* \
    ${prefix}/lib/cmake/* \
"

# Documentation / metadata (relocated)
FILES:${PN}-doc = "${docdir}/${PN}/*"

# License directory (explicit)
FILES:${PN}-doc += "${datadir}/licenses/${PN}/LICENSE"

## Prebuilt (already stripped) – suppress only what’s necessary.
## Keep scope narrow so other QA checks still run.
INSANE_SKIP:${PN} += " already-stripped ldflags"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_SYSROOT_STRIP = "1"