SUMMARY = "DEEPX Runtime SDK"
DESCRIPTION = "DX-M1 Runtime Library and CLI tools"
HOMEPAGE = "https://deepx.ai/"
LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://LICENSE;md5=df0ebe3edba67d21cb2e798ef0ee2905"

# do_fetch source
SRC_URI = "git://github.com/DEEPX-AI/dx_rt.git;protocol=https;branch=main \
            file://0101-fix-terminate.patch \
            file://0102-python-build.patch \
            file://setup_1.1.4.py \
        "
SRCREV = "6a0052e6266f4b0a13d001fb90e5a7721d57520d"

S = "${WORKDIR}/git"

PACKAGECONFIG[shared_dxrt_lib] = "\
    -DUSE_SHARED_DXRT_LIB=ON"

inherit cmake python3-dir python3native

# onnxruntime prebuilt library
DEPENDS += "libonnxruntime"
DEPENDS += "python3 python3-pybind11"
DEPENDS += "python3-native chrpath-native"
DEPENDS += "python3-pip-native python3-wheel-native"

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
    -DUSE_PYTHON=ON \
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

do_configure:prepend() {
    # Copy setup.py from WORKDIR to the python_package folder in the git source
    cp ${WORKDIR}/setup_1.1.4.py ${S}/python_package/setup.py

    # 2. Remove existing pyproject.toml (to prevent conflicts)
    # -f option: Proceed without error even if the file does not exist
    rm -f ${S}/python_package/pyproject.toml
}

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

# Execute pip install (Run after CMake installation)
do_install:append() {
    # Create destination directory (site-packages)
    install -d ${D}${PYTHON_SITEPACKAGES_DIR}

    # Execute pip install
    # --no-deps: Do not install dependencies (Yocto manages them via RDEPENDS)
    # --target: Specify install path to DESTDIR (${D}) for image creation
    # --no-cache-dir: Do not use cache
    
    cd ${S}/python_package
    python3 -m pip install . \
        --no-deps \
        --no-cache-dir \
        --target="${D}${PYTHON_SITEPACKAGES_DIR}"

    # Delete metadata files causing buildpaths issues
    # Find and delete direct_url.json inside *.dist-info folders
    find ${D}${PYTHON_SITEPACKAGES_DIR} -name "direct_url.json" -delete
}

FILES:${PN} += "${libdir}/python* ${PYTHON_SITEPACKAGES_DIR}"
INSANE_SKIP:${PN} += "already-stripped"


# added run_model.py
do_install:append() {
    # Create target directory (/etc/dx-rt)
    # ${sysconfdir} usually points to /etc
    install -d ${D}${sysconfdir}/dx-rt

    # Copy file
    # -m 0755: Grant execution permission (Recommended since it is a script)
    # ${S}: Source directory
    # ${D}: Image installation directory
    install -m 0755 ${S}/python_package/cli/run_model.py ${D}${sysconfdir}/dx-rt/
}

# added run_model.py
FILES:${PN} += "${sysconfdir}/dx-rt/run_model.py"

