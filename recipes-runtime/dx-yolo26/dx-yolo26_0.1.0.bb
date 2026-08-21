SUMMARY = "DeepX M1 YOLO26 Applications"
DESCRIPTION = "YOLO26 applications for the M1 NPU - object detection, classification, \
pose estimation, instance segmentation and depth estimation - built on DXRT (DeepX Runtime). \
The YOLO26 postprocessing is compiled into the applications; no shared library is built."
HOMEPAGE = "https://github.com/DEEPX-AI/dx_yolo26/"
LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://LICENSE;md5=df0ebe3edba67d21cb2e798ef0ee2905"

# 1. Source Definition
SRC_URI = "git://github.com/DEEPX-AI/dx_yolo26.git;protocol=https;branch=main"
# v0.1.0 + the GCC 13 / OpenCV-without-dnn build fixes (tag v0.1.1 once cut)
SRCREV = "555cc3551838b5cf316638012a5089a81eaa94fe"

S = "${WORKDIR}/git"

# 2. Build-time Dependencies
# dx-rt provides libdxrt and the dxrt headers, OpenCV is used for image I/O and
# for the visualization. cxxopts and nlohmann/json are vendored in the source tree.
DEPENDS = " \
    dx-rt \
    opencv \
"

inherit cmake

# 3. Runtime Dependencies
RDEPENDS:${PN} += " \
    dx-rt \
    opencv \
"

# Which runner variant of every application is built: "async" (default), "sync"
# or "both". Everything the build creates is also installed, so this is the only
# knob needed to control the package contents.
DXYOLO26_VARIANTS ?= "async"

# DXRT_INSTALLED_DIR is used with find_library()/find_path() and has to point at
# the recipe sysroot. DXYOLO26_DATA_DIR is compiled into the applications as the
# installed data directory their error hints point at - it must be the on-target
# path, not the build directory, or the buildpaths QA check trips.
EXTRA_OECMAKE = " \
    -DDXRT_INSTALLED_DIR=${STAGING_DIR_HOST}${prefix} \
    -DDXYOLO26_DATA_DIR=${datadir}/dx_yolo26 \
    -DDXYOLO26_VARIANTS=${DXYOLO26_VARIANTS} \
    -DCMAKE_SKIP_RPATH=ON \
    -DCMAKE_SKIP_INSTALL_RPATH=ON \
"

# Packaging Configuration
# ${PN} carries the applications and their postprocess parameters. The sample input images are split out into ${PN}-samples (~1 MB): an
# application called without -i/-v/-c/-r falls back to "sample/img/..." relative to
# the working directory, so they are only needed to run a demo straight from
# ${datadir}/dx_yolo26 - see also the dx-yolo26-sample recipe for the full bundle
# with models and videos.
PACKAGE_BEFORE_PN = "${PN}-samples"

FILES:${PN}-samples = "${datadir}/dx_yolo26/sample"

FILES:${PN} += " \
    ${bindir}/yolo26* \
    ${datadir}/dx_yolo26/examples \
"

# NOTE: .dxnn models are not part of this package. Copy them onto the target from
# DX-ModelZoo and pass them with "-m <model.dxnn>", or place them under
# assets/models/ relative to the working directory to use an application default.
