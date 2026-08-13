SUMMARY = "DeepX M1 Streaming Demonstration Application"
DESCRIPTION = "A demonstration application utilizing the DXRT (DeepX Runtime) for stream processing and inference on the M1 NPU."
HOMEPAGE = "https://github.com/DEEPX-AI/dx_stream/"
LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://LICENSE;md5=df0ebe3edba67d21cb2e798ef0ee2905"

# 1. Source Definition
# Specify the GitHub URL and the exact commit hash for reproducible builds.
SRC_URI = "git://github.com/DEEPX-AI/dx_stream.git;protocol=https;branch=main"
SRCREV = "33024ecc8824b3b4fdbf054eaf679e959745b1bb"

S = "${WORKDIR}/git"

# 2. Build-time Dependencies
# List the libraries required for compiling the source code (including development headers).
# These must align with the dependencies declared in the project's meson.build file.
DEPENDS = "meson-native ninja-native"
DEPENDS += " \
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
    libeigen \
"

# Inherit the meson.bbclass to handle the Meson build system.
inherit meson pkgconfig

MESON_SOURCEPATH = "${S}/gst-dxstream-plugin"

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

# Build custom_library after gst-dxstream-plugin
do_compile:append() {
    bbnote "Installing gst-dxstream-plugin headers for custom_library..."

    # Install gst-dxstream-plugin with correct prefix
    cd ${B}
    meson configure --prefix=/usr
    DESTDIR="${S}/install" meson install

    bbnote "Building custom_library postprocess libraries..."

    # Set PKG_CONFIG_PATH and paths for gstdxstream linking
    export PKG_CONFIG_PATH="${S}/install/usr/lib/pkgconfig:${PKG_CONFIG_PATH}"
    export CPATH="${S}/install/usr/include:${CPATH}"
    export LIBRARY_PATH="${S}/install/usr/lib/gstreamer-1.0:${S}/install/usr/lib:${LIBRARY_PATH}"
    export LD_LIBRARY_PATH="${S}/install/usr/lib/gstreamer-1.0:${S}/install/usr/lib:${LD_LIBRARY_PATH}"

    # Build each postprocess library with proper cross-compile settings
    for subdir in ${S}/dx_stream/custom_library/postprocess_library/*/; do
        if [ -d "$subdir" ] && [ -f "$subdir/meson.build" ]; then
            bbnote "Building $(basename $subdir)..."
            cd "$subdir"

            meson setup build \
                --cross-file=${WORKDIR}/meson.cross \
                --buildtype=release \
                --prefix=/usr \
                -Dcpp_args="-I${S}/install/usr/include" \
                -Dc_args="-I${S}/install/usr/include" \
                -Dcpp_link_args="-L${S}/install/usr/lib/gstreamer-1.0 -L${S}/install/usr/lib" \
                -Dc_link_args="-L${S}/install/usr/lib/gstreamer-1.0 -L${S}/install/usr/lib"

            ninja -C build
            DESTDIR="${S}/install" ninja -C build install
            rm -rf build
        fi
    done
}

do_install() {
    # NOTE: Defining do_install() overrides meson.bbclass's do_install
    # (meson_do_install), so the main gst-dxstream-plugin would NOT be
    # installed unless we call it explicitly here. Without this, only the
    # custom_library .so files end up in ${D} and libgstdxstream.so* /
    # headers / gstdxstream.pc are missing, which also breaks the
    # "shared library provider for libgstdxstream.so.0" lookup.
    meson_do_install

    bbnote "Installing custom_library postprocess libraries..."
    
    # Install custom_library (follows build.sh: share/gstdxstream/lib)
    if [ -d "${S}/install/usr/share/gstdxstream/lib" ]; then
        install -d ${D}${datadir}/gstdxstream/lib
        cp -r ${S}/install/usr/share/gstdxstream/lib/* ${D}${datadir}/gstdxstream/lib/
    fi
    
    # Fallback: if postprocess is in lib/postprocess (old structure)
    if [ -d "${S}/install/usr/lib/postprocess" ]; then
        install -d ${D}${datadir}/gstdxstream/lib
        cp -r ${S}/install/usr/lib/postprocess/* ${D}${datadir}/gstdxstream/lib/
    fi
}

# Packaging Configuration
FILES:${PN} += " \
    ${libdir}/gstreamer-1.0/*.so* \
    ${datadir}/gstdxstream/* \
"

FILES:${PN}-dev += " \
    ${includedir}/gstdxstream/* \
    ${libdir}/pkgconfig/gstdxstream.pc \
"

# Disable QA checks for plugins that depend on files within same package
INSANE_SKIP:${PN} += "libdir file-rdeps dev-so ldflags"
INSANE_SKIP:${PN}-dbg += "libdir"
