SUMMARY = "DeepX M1 Streaming Demonstration Application"
DESCRIPTION = "A demonstration application utilizing the DXRT (DeepX Runtime) for stream processing and inference on the M1 NPU."
HOMEPAGE = "https://github.com/DEEPX-AI/dx_stream/"
LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://LICENSE;md5=df0ebe3edba67d21cb2e798ef0ee2905"

# Inherit the meson.bbclass to handle the Meson build system.
inherit meson

# 1. Source Definition
# Specify the GitHub URL and the exact commit hash for reproducible builds.
SRC_URI = "git://github.com/DEEPX-AI/dx_stream.git;protocol=https;branch=main"
SRCREV = "875b63eec792192822e088f3c6324e55a945b58d"

# Inherit the meson.bbclass to handle the Meson build system.
inherit meson pkgconfig



#PV = "1.0.0+git${SRCPV}" # Set the package version based on the Git information.
#PV = "2.1.0"
S = "${WORKDIR}/git"
MESON_SOURCEPATH = "${S}/gst-dxstream-plugin"

# 2. V3 Mode Configuration (Conditional Build Option)
# In Yocto, we use PACKAGECONFIG or a simple variable to handle options like '--v3'.
# Default is off, but can be enabled via local.conf or by uncommenting below:
PACKAGECONFIG ??= ""
PACKAGECONFIG[v3] = "-Dv3_mode=true,-Dv3_mode=false"

# Alternatively, if you want it always based on a variable:
EXTRA_MESON_ARGS += "${@bb.utils.contains('PACKAGECONFIG', 'v3', '-Dv3_mode=true', '-Dv3_mode=false', d)}"


# 2. Build-time Dependencies
# List the libraries required for compiling the source code (including development headers).
# These must align with the dependencies declared in the project's meson.build file.
DEPENDS = "meson-native ninja-native"
DEPENDS = " \
    dx-rt \
    gstreamer1.0 \
    gstreamer1.0-plugins-base \
    opencv \
    glib-2.0 \
    json-glib \
    libyuv \
    mosquitto \
    librdkafka \
    pkgconfig-native \
"

# 3. Runtime Dependencies
# List the shared libraries required for the built application to execute successfully on the target device.
RDEPENDS:${PN} = " \
    dx-rt \
    gstreamer1.0 \
    gstreamer1.0-plugins-base \
    opencv \
    json-glib \
    libyuv \
    mosquitto \
    librdkafka \
"

# Meson Build Options
# Define any extra options to pass to the Meson build system, if necessary.
# For example, to enable/disable specific features defined in meson_options.txt.
# EXTRA_MESON_ARGS = "-Dfeature_name=enabled"

# Meson handles the installation typically to /usr/bin for executables and 
# /usr/lib for libraries, so a custom do_install function is usually not needed.


# Build custom_library after gst-dxstream-plugin
do_compile:append() {
    bbnote "Installing gst-dxstream-plugin headers for custom_library..."

    # Install gst-dxstream-plugin with correct prefix
    cd ${B}
    meson configure --prefix=${S}/install
    DESTDIR="" meson install

    bbnote "Building custom_library postprocess libraries..."

    # Build each postprocess library with proper cross-compile settings
    for subdir in ${S}/dx_stream/custom_library/postprocess_library/*/; do
        if [ -d "$subdir" ] && [ -f "$subdir/meson.build" ]; then
            bbnote "Building $(basename $subdir)..."
            cd "$subdir"

            meson setup build \
                --buildtype=release \
                --prefix=${S}/install \
                --cross-file ${WORKDIR}/meson.cross

            ninja -C build
            DESTDIR="" ninja -C build install
            rm -rf build
        fi
    done
}

do_install() {
    # Install gst-dxstream-plugin outputs (from meson install)
    # Meson already installs to ${D}, so just copy additional custom_library outputs

    # install dx_stream to /usr/lib
    if [ -d "${S}/install/lib/gstreamer-1.0" ]; then
        install -d ${D}${libdir}/gstreamer-1.0
        cp -r ${S}/install/lib/gstreamer-1.0/* ${D}${libdir}/gstreamer-1.0/ || true
    fi

    # Install postprocess plugins to libdir instead of datadir to avoid QA warning
    if [ -d "${S}/install/lib/postprocess" ]; then
        install -d ${D}${datadir}/dx-stream/lib
        cp -r ${S}/install/lib/postprocess/* ${D}${datadir}/dx-stream/lib/ || true
    fi

    # include
    if [ -d "${S}/install/include" ]; then
        install -d ${D}${prefix}/include
        cp -r ${S}/install/include/* ${D}${prefix}/include/ || true
    fi
}

# Packaging Configuration
FILES:${PN} += " \
    ${libdir}/gstreamer-1.0/*.so \
    ${datadir}/dx-stream/* \
"

FILES:${PN}-dev += " \
    ${includedir}/* \
"

FILES:${PN}-staticdev += "${libdir}/gstreamer-1.0/*.a"

# Disable QA checks for plugins that depend on files within same package
INSANE_SKIP:${PN} += "libdir file-rdeps"
INSANE_SKIP:${PN}-dbg += "libdir"

