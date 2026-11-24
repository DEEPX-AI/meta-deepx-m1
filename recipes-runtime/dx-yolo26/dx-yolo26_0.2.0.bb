SUMMARY = "DeepX M1 YOLO26 Applications"
DESCRIPTION = "YOLO26 applications for the M1 NPU - object detection, classification, \
pose estimation, instance segmentation and depth estimation - built on DXRT (DeepX Runtime), \
plus the *_preopt variants of detection, pose and segmentation for the pre-optimized models \
that select the top-k candidates inside the model. The YOLO26 postprocessing is compiled \
into the applications; no shared library is built."
HOMEPAGE = "https://github.com/DEEPX-AI/dx_yolo26/"
# DEEPX code is proprietary. Two MIT header-only libraries are vendored in the
# source tree and compiled into the applications: cxxopts and nlohmann/json.
# Their license headers are part of LIC_FILES_CHKSUM so they reach the license
# manifest (tmp/deploy/licenses).
LICENSE = "Proprietary & MIT"
LIC_FILES_CHKSUM = " \
    file://LICENSE;md5=df0ebe3edba67d21cb2e798ef0ee2905 \
    file://extern/cxxopts.hpp;beginline=1;endline=23;md5=94bcf26173cdc3ba81ec8e9cd00aadd2 \
    file://src/cpp_example/common/third_party/nlohmann_json.hpp;beginline=1;endline=7;md5=70c6b2fa6e75c5d655d5e5b996dc7b23 \
"

# 1. Source Definition
SRC_URI = "git://github.com/DEEPX-AI/dx_yolo26.git;protocol=https;branch=main"
# tag: v0.2.0
SRCREV = "04750a46fc5ec6fb15173421f80b97c30e0e399f"

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
    -DDXYOLO26_INSTALL_SAMPLES=OFF \
    -DCMAKE_SKIP_RPATH=ON \
    -DCMAKE_SKIP_INSTALL_RPATH=ON \
"

# Packaging Configuration
# ${PN} carries the applications and their postprocess parameters. The sample
# images in the source tree are not installed (-DDXYOLO26_INSTALL_SAMPLES=OFF):
# dx-yolo26-sample ships them together with the models and the video clips, so
# shipping both would put two copies of the same images in the image.
FILES:${PN} += " \
    ${bindir}/yolo26* \
    ${datadir}/dx_yolo26/examples \
"

# NOTE: .dxnn models are not part of this package. Copy them onto the target from
# DX-ModelZoo and pass them with "-m <model.dxnn>", or place them under
# assets/models/ relative to the working directory to use an application default.

# License texts, shipped in the main package: the DEEPX LICENSE and the MIT
# notices of the vendored cxxopts and nlohmann/json headers.
do_install:append() {
    install -d ${D}${datadir}/licenses/${PN}
    install -m 0644 ${S}/LICENSE ${D}${datadir}/licenses/${PN}/LICENSE
    {
        echo "dx-yolo26 includes the following third-party software."
        echo
        echo "== cxxopts (MIT) =="
        sed -n '1,23p' ${S}/extern/cxxopts.hpp
        echo
        echo "== JSON for Modern C++ / nlohmann/json (MIT) =="
        sed -n '1,7p' ${S}/src/cpp_example/common/third_party/nlohmann_json.hpp
        echo
        cat ${COMMON_LICENSE_DIR}/MIT
    } > ${D}${datadir}/licenses/${PN}/THIRD_PARTY_NOTICES.txt
    chmod 0644 ${D}${datadir}/licenses/${PN}/THIRD_PARTY_NOTICES.txt
}

FILES:${PN} += "${datadir}/licenses/${PN}"
