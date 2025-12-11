SUMMARY = "DEEPX Runtime SDK"
DESCRIPTION = "DX-M1 Runtime Library and CLI tools"
HOMEPAGE = "https://deepx.ai/"
LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://LICENSE;md5=df0ebe3edba67d21cb2e798ef0ee2905"

# do_fetch source
SRC_URI = "git://github.com/DEEPX-AI/dx_rt.git;protocol=https;branch=main"
SRCREV = "969742dbf71bdb6a08cf23f2260f88284f280c86"

S = "${WORKDIR}/git"

PACKAGECONFIG[shared_dxrt_lib] = "\
    -DUSE_SHARED_DXRT_LIB=ON"

inherit cmake

# onnxruntime prebuilt library
DEPENDS += "dx-ort"
DEPENDS += "chrpath-native"
DEPENDS += "python3"
DEPENDS += "python3-native"

# ------------------------------------------------------------------
# CMake feature toggles to avoid configure failure:
# Upstream CMake defaults currently turn ON USE_ORT and USE_SERVICE.
# ONNX Runtime libs are not provided yet, causing ONNXLIB_DIRS-NOTFOUND.
# Disable those optional features explicitly and build shared library only.
# Also disable tests to speed up build if they depend on ORT/service.
# ------------------------------------------------------------------
EXTRA_OECMAKE += " \
    -DUSE_ORT=ON \
    -DUSE_SERVICE=OFF \
    -DUSE_SHARED_DXRT_LIB=ON \
    -DUSE_DXRT_TEST=OFF \
    -DBUILD_SHARED_LIBS=ON \
    -DCMAKE_SKIP_RPATH=ON \
    -DCMAKE_SKIP_INSTALL_RPATH=ON \
    -DCMAKE_BUILD_WITH_INSTALL_RPATH=OFF \
    -DCMAKE_INSTALL_RPATH= \ 
    -DCMAKE_BUILD_RPATH= \ 
"

# add new sub-package (${PN} / ${PN}-dev / ${PN}-dbg)
PACKAGES:append = " ${PN}-cli ${PN}-examples"

FILES_SOLIBSDEV = ""
FILES:${PN} += "${libdir}/libdxrt.so"

# CLI package
FILES:${PN}-cli = " \
    ${bindir}/dxrt-cli \
    ${bindir}/dxrtd \
    ${bindir}/parse_model \
    ${bindir}/run_model \
    ${bindir}/dxtop \
    ${bindir}/dxbenchmark \
"

# examples
FILES:${PN}-examples = "${bindir}/examples/*"
RDEPENDS:${PN}-examples = "${PN}-cli"
INSANE_SKIP:${PN} += "installed-vs-shipped"

do_install:append() {
    if [ -d "${D}/media" ]; then
        echo "INFO: dx-runtime: Removing problematic host path /media from image root."
        rm -rf "${D}/media"
    fi

    # remove .h.in
    rm -f ${D}${includedir}/dxrt/gen.h.in || true

    if [ -d "${D}/home" ]; then
        echo "INFO: dx-runtime: Removing problematic host path /mnt from image root."
        rm -rf "${D}/home"
    fi

    for b in dxrt-cli dxrtd run_model parse_model dxtop dxbenchmark; do
        if [ -f "${D}${bindir}/$b" ]; then
            chrpath -d "${D}${bindir}/$b" 2>/dev/null || true
        fi
    done
}

