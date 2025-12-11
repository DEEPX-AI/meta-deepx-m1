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
inherit meson



#PV = "1.0.0+git${SRCPV}" # Set the package version based on the Git information.
#PV = "2.1.0"
S = "${WORKDIR}/git"
MESON_SOURCEPATH = "${S}/gst-dxstream-plugin"

# 2. V3 Mode Configuration (Conditional Build Option)
# In Yocto, we use PACKAGECONFIG or a simple variable to handle options like '--v3'.
# Default is off, but can be enabled via local.conf or by uncommenting below:
# PACKAGECONFIG ??= "v3"
PACKAGECONFIG[v3] = "-Dv3_mode=true,-Dv3_mode=false"

# Alternatively, if you want it always based on a variable:
EXTRA_MESON_ARGS += "${@bb.utils.contains('PACKAGECONFIG', 'v3', '-Dv3_mode=true', '-Dv3_mode=false', d)}"


# 2. Build-time Dependencies
# List the libraries required for compiling the source code (including development headers).
# These must align with the dependencies declared in the project's meson.build file.
DEPENDS = " \
    dx-runtime \
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
    dx-runtime \
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

# V3 Option
PACKAGECONFIG ??= ""
PACKAGECONFIG[v3] = "-Dv3_mode=true,-Dv3_mode=false"
EXTRA_MESON_ARGS += "${@bb.utils.contains('PACKAGECONFIG', 'v3', '-Dv3_mode=true', '-Dv3_mode=false', d)}"

# 3. [FIX] Packaging Configuration
# Include the GStreamer plugin shared object in the main package
FILES:${PN} += "${libdir}/gstreamer-1.0/*.so"

# (If static libraries are generated, put them in staticdev)
FILES:${PN}-staticdev += "${libdir}/gstreamer-1.0/*.a"