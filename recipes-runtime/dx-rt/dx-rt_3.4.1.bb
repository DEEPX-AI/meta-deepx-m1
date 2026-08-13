SUMMARY = "DEEPX Runtime SDK"
DESCRIPTION = "DX-M1 Runtime Library and CLI tools"
HOMEPAGE = "https://deepx.ai/"
LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://LICENSE;md5=df0ebe3edba67d21cb2e798ef0ee2905"

# do_fetch source
SRC_URI = "git://github.com/DEEPX-AI/dx_rt.git;protocol=https;branch=main \
            file://0301-python-build.patch \
            file://setup_3.4.0.py \
            file://dxrt-init \
        "
SRCREV = "baec91445dea9aa881b5bb0b4867ba075aeba4ae"

S = "${WORKDIR}/git"

PACKAGECONFIG[shared_dxrt_lib] = "\
    -DUSE_SHARED_DXRT_LIB=ON"

inherit cmake python3-dir python3native update-rc.d

INITSCRIPT_NAME = "dxrt-init"
INITSCRIPT_PARAMS = "defaults 90"

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
    -DUSE_SERVICE=ON \
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

# setting systemd service
#SYSTEMD_PACKAGES = "${PN}-cli"
#SYSTEMD_SERVICE:${PN}-cli = "dxrt.service"
#SYSTEMD_AUTO_ENABLE:${PN}-cli = "enable"

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

# examples & cli
FILES:${PN}-examples = "${bindir}/examples/*"
RDEPENDS:${PN}-cli += "ncurses-terminfo-base ncurses-libncurses"
RDEPENDS:${PN}-examples = "${PN}-cli"
INSANE_SKIP:${PN} += "installed-vs-shipped"

do_configure:prepend() {
    # Copy setup.py from UNPACKDIR to the python_package folder in the git source
    # (walnascar: SRC_URI local files unpack to UNPACKDIR, not WORKDIR)
    cp ${UNPACKDIR}/setup_3.4.0.py ${S}/python_package/setup.py

    # 2. Remove existing pyproject.toml (to prevent conflicts)
    # -f option: Proceed without error even if the file does not exist
    rm -f ${S}/python_package/pyproject.toml
}

do_install:append() {

    # system service
    #if ${@bb.utils.contains('DISTRO_FEATURES','systemd','true','false',d)}; then
    #    install -d ${D}${systemd_system_unitdir}
    #    install -m 0644 ${UNPACKDIR}/dxrt.service ${D}${systemd_system_unitdir}
    #fi

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

    # install SysVinit script
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${UNPACKDIR}/dxrt-init ${D}${sysconfdir}/init.d/dxrt-init

    # Create a script to set environment variables automatically on login
    install -d ${D}${sysconfdir}/profile.d
    
    # Generate shell script for TERMINFO path and TERM setting
    # Using 0644 as it will be sourced by the shell, not executed directly
    cat <<EOF > ${D}${sysconfdir}/profile.d/dxrt_env.sh
#!/bin/sh
export TERMINFO=/etc/terminfo
EOF
    chmod 0644 ${D}${sysconfdir}/profile.d/dxrt_env.sh

}

FILES:${PN}-cli += "${sysconfdir}/init.d/dxrt-init"

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



