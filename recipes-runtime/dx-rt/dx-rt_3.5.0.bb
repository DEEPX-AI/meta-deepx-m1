SUMMARY = "DEEPX Runtime SDK"
DESCRIPTION = "DX-M1 Runtime Library and CLI tools"
HOMEPAGE = "https://deepx.ai/"
# DEEPX code is proprietary. The binaries also contain third-party code listed in
# NOTICE.md: RapidJSON, cxxopts and ncurses (MIT / MIT-style), pybind11 (BSD-3-Clause).
# NOTICE.md is part of LIC_FILES_CHKSUM so it reaches the license manifest
# (tmp/deploy/licenses), and it is installed on the target next to LICENSE.
# If upstream changes NOTICE.md (DXM1-2902), update its md5 here.
LICENSE = "Proprietary & MIT & BSD-3-Clause"
LIC_FILES_CHKSUM = " \
    file://LICENSE;md5=df0ebe3edba67d21cb2e798ef0ee2905 \
    file://NOTICE.md;md5=a994d1ebd6d021417127aaf16d17d13b \
"

# do_fetch source
SRC_URI = "git://github.com/DEEPX-AI/dx_rt.git;protocol=https;branch=main \
            file://0401-python-build.patch \
            file://setup_3.5.0.py \
            file://dxrt-init \
            file://dxrt.service \
        "
SRCREV = "f997363c97015cd3a601e99d3d0c91bcc1be70c6"

S = "${WORKDIR}/git"

PACKAGECONFIG[shared_dxrt_lib] = "\
    -DUSE_SHARED_DXRT_LIB=ON"

# ------------------------------------------------------------------
# Service / init manager selection per MACHINE
#   smarc-rzg3e (Renesas RZ/G3E, systemd) : dxrt.service via systemctl
#   others (Raspberry Pi etc.)            : SysVinit script (dxrt-init)
# ------------------------------------------------------------------
DXRT_USE_SYSTEMD = "0"
DXRT_USE_SYSTEMD:smarc-rzg3e = "1"

inherit cmake python3-dir python3native
inherit ${@'systemd' if d.getVar('DXRT_USE_SYSTEMD') == '1' else 'update-rc.d'}

# SysVinit (non smarc-rzg3e)
INITSCRIPT_NAME = "dxrt-init"
INITSCRIPT_PARAMS = "defaults 90"

# systemd (smarc-rzg3e)
# The service must belong to ${PN}: ${PN} is listed before ${PN}-cli in
# PACKAGES, so dxrtd and the unit file are packaged into ${PN}, and images
# install only "dx-rt". Binding it to ${PN}-cli left the unit out of the
# image (dxrt.service not found, never enabled at boot).
SYSTEMD_PACKAGES = "${PN}"
SYSTEMD_SERVICE:${PN} = "dxrt.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

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
    ${bindir}/dxtop.bin \
    ${bindir}/dxbenchmark \
"

# examples & cli
FILES:${PN}-examples = "${bindir}/examples/*"
RDEPENDS:${PN}-cli += "ncurses-terminfo-base ncurses-libncurses"
RDEPENDS:${PN}-examples = "${PN}-cli"
INSANE_SKIP:${PN} += "installed-vs-shipped"

do_configure:prepend() {
    # Copy setup.py from WORKDIR to the python_package folder in the git source
    cp ${WORKDIR}/setup_3.5.0.py ${S}/python_package/setup.py

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

    # Upstream CMake may install into the absolute build path (${S}/...),
    # e.g. /data/yocto/... on build hosts that do not build under /home.
    if [ -d "${D}${S}" ]; then
        leak_top="$(echo "${S}" | cut -d/ -f2)"
        case "$leak_top" in
            ""|usr|etc|lib|bin|sbin|var|opt) ;;
            *)
                echo "INFO: dx-runtime: Removing problematic host path /$leak_top from image root."
                rm -rf "${D}/$leak_top"
                ;;
        esac
    fi

    for b in dxrt-cli dxrtd run_model parse_model dxtop dxbenchmark; do
        if [ -f "${D}${bindir}/$b" ]; then
            chrpath -d "${D}${bindir}/$b" 2>/dev/null || true
        fi
    done

    # Create a script to set environment variables automatically on login
    # Using 0644 as it will be sourced by the shell, not executed directly
    install -d ${D}${sysconfdir}/profile.d

    if [ "${DXRT_USE_SYSTEMD}" = "1" ]; then
        # ---- smarc-rzg3e : systemd service (started by systemctl) ----
        install -d ${D}${systemd_system_unitdir}
        install -m 0644 ${WORKDIR}/dxrt.service ${D}${systemd_system_unitdir}/dxrt.service
        # Do NOT install /etc/init.d/dxrt-init here: systemd-sysv-generator
        # would otherwise create a second unit and start dxrtd twice.

        # ---- dxtop wrapper (ncurses terminfo) ----
        # dxtop statically links upstream's prebuilt libncursesw.a, whose
        # built-in terminfo path is a developer host path that does not exist
        # on target. It only finds terminfo through TERMINFO / TERMINFO_DIRS.
        # profile.d is sourced by login shells only (ssh), so non-login shells
        # (weston-terminal, su) failed with "Error opening terminal: xterm".
        # Install the real binary as dxtop.bin and wrap it so it works from
        # any shell.
        if [ -f "${D}${bindir}/dxtop" ]; then
            mv ${D}${bindir}/dxtop ${D}${bindir}/dxtop.bin
            cat <<'EOF' > ${D}${bindir}/dxtop
#!/bin/sh
# DEEPX dx-rt : dxtop wrapper
# dxtop uses a statically linked ncurses that does not search the system
# terminfo database by default. Point it there unless the user set one.
if [ -z "$TERMINFO" ] && [ -z "$TERMINFO_DIRS" ]; then
    TERMINFO_DIRS=/etc/terminfo:/usr/share/terminfo
    export TERMINFO_DIRS
fi
exec ${bindir}/dxtop.bin "$@"
EOF
            chmod 0755 ${D}${bindir}/dxtop
        fi

        # ---- terminal settings for login shells ----
        # - TERMINFO=/etc/terminfo : same as before (used by the static ncurses)
        # - serial-getty (ttySC0) logs in with TERM=vt220 (no color / limited
        #   capabilities), so promote it to xterm-256color, which is shipped
        #   in ncurses-terminfo-base.
        cat <<'EOF' > ${D}${sysconfdir}/profile.d/dxrt_env.sh
#!/bin/sh
# DEEPX dx-rt : terminal environment for dxtop (ncurses)
export TERMINFO=/etc/terminfo

case "$TERM" in
    ""|dumb|unknown|vt100|vt102|vt220)
        export TERM=xterm-256color
        ;;
esac
EOF
    else
        # ---- others (Raspberry Pi etc.) : SysVinit script (unchanged) ----
        install -d ${D}${sysconfdir}/init.d
        install -m 0755 ${WORKDIR}/dxrt-init ${D}${sysconfdir}/init.d/dxrt-init

        # Generate shell script for TERMINFO path
        cat <<EOF > ${D}${sysconfdir}/profile.d/dxrt_env.sh
#!/bin/sh
export TERMINFO=/etc/terminfo
EOF
    fi
    chmod 0644 ${D}${sysconfdir}/profile.d/dxrt_env.sh

}

FILES:${PN}-cli += "${sysconfdir}/init.d/dxrt-init"
FILES:${PN} += "${systemd_system_unitdir}/dxrt.service"

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

# License and third-party notices, shipped in the main package so they are on
# every device that carries the runtime.
do_install:append() {
    install -d ${D}${datadir}/licenses/${PN}
    install -m 0644 ${S}/LICENSE ${D}${datadir}/licenses/${PN}/LICENSE
    install -m 0644 ${S}/NOTICE.md ${D}${datadir}/licenses/${PN}/NOTICE.md
}

FILES:${PN} += "${datadir}/licenses/${PN}"
