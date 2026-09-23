SUMMARY = "DeepX M1 YOLO26 Sample Assets"
DESCRIPTION = "Sample input images, videos and .dxnn model files for the YOLO26 \
applications shipped by dx-yolo26. Detection, pose estimation and instance segmentation \
use the pre-optimized models (top-k selection inside the model), so this bundle needs \
the *_preopt applications of dx-yolo26 0.2.0 or later."
SECTION = "examples"
LICENSE = "CLOSED"

# 1. Source Definition
# The bundle - models, video clips and still images - ships with this layer:
#   recipes-runtime/dx-yolo26-sample/files/dx-yolo26-sample_${PV}.tar.gz
#
# It is a ~36 MB binary blob, so every new version adds that much to the layer
# history. A release asset would avoid that, but dx_yolo26 is a private repository:
# fetching one needs GitHub credentials on every build host, which a file in the
# layer does not. Revisit this if the repository becomes public or an internal
# mirror is available:
#   SRC_URI = "https://<host>/dx-yolo26-sample_${PV}.tar.gz;unpack=0"
#   SRC_URI[sha256sum] = "<sha256sum of the tarball>"
#
# The tarball is expected to contain a single top-level directory (hence
# --strip-components=1 below) laid out the way the applications look for their
# inputs at run time:
#
#   <top>/assets/models/*.dxnn   models, named as each factory's getDefaultModel():
#                                  pre_optimized_yolo26-n-od.dxnn  (yolo26n_preopt)
#                                  yolo26-n-cls_224x224.dxnn       (yolo26n_cls)
#                                  pre_optimized_yolo26n-pose.dxnn (yolo26n_pose_preopt)
#                                  pre_optimized_yolo26n-seg.dxnn  (yolo26n_seg_preopt)
#                                  yolo26-depth-n_768x768.dxnn     (yolo26n_depth)
#                                Detection, pose and segmentation ship the
#                                pre-optimized models: the top-k 300 selection runs
#                                inside the model, so the application has almost no
#                                postprocessing left. The plain yolo26n / yolo26n_pose /
#                                yolo26n_seg applications are still installed by
#                                dx-yolo26 but need their own model via -m.
#   <top>/assets/videos/*        video clips, named as the -v defaults expect:
#                                  snowboard.mp4            (yolo26n_preopt)
#                                  dogs.mp4                 (yolo26n_cls, yolo26n_seg_preopt)
#                                  dance-solo.mov           (yolo26n_pose_preopt)
#                                  blackbox-city-road.mp4   (yolo26n_depth)
#   <top>/sample/img/*           still images used when no input option is given:
#                                  sample_street.jpg   (yolo26n_preopt, yolo26n_seg_preopt)
#                                  sample_dog.jpg      (yolo26n_cls)
#                                  sample_people.jpg   (yolo26n_pose_preopt)
#                                  sample_parking.jpg  (yolo26n_depth)
#   <top>/run.sh                 "run.sh <0-4>" runs one application on its video
#                                clip; POSIX sh, so it needs no bash on the target
#
# unpack=0 keeps bitbake from extracting it: do_install unpacks it by hand so the
# host UID/GID and the permissions recorded in the tarball are discarded.
SRC_URI = "file://dx-yolo26-sample_${PV}.tar.gz;unpack=0"

S = "${WORKDIR}"

# 2. Installation path
# Same location convention as dx-stream-sample. The applications resolve both their
# default model ("assets/models/<file>", declared by the application's factory) and
# their default input image ("sample/img/<file>") relative to the working directory,
# so keeping the bundle layout intact makes
#   cd ${SAMPLE_DEST_DIR} && yolo26n_preopt_async
# work with no arguments at all.
SAMPLE_DEST_DIR = "${sysconfdir}/dx-yolo26-sample"

# Images, videos and .dxnn files are architecture independent, so the package is
# built once and shared by every MACHINE.
inherit allarch

# 3. Runtime Dependencies
# The assets are useless without the applications that consume them.
RDEPENDS:${PN} += "dx-yolo26"
# run.sh is POSIX sh (/bin/sh), so no bash dependency is needed. Uncomment if a
# bash-only script is ever added to the bundle:
#RDEPENDS:${PN} += "bash"
#INSANE_SKIP:${PN} += "file-rdeps"

do_install() {
    install -d ${D}${SAMPLE_DEST_DIR}

    # Extract with flags that discard host UID/GID and umask-dependent modes:
    #   --no-same-owner        : do not restore the UID/GID stored in the tarball
    #   --no-same-permissions  : do not restore odd permission bits
    tar --no-same-owner --no-same-permissions \
        -xf ${WORKDIR}/dx-yolo26-sample_${PV}.tar.gz \
        -C ${D}${SAMPLE_DEST_DIR} --strip-components=1

    # Force root ownership and predictable modes regardless of how the bundle
    # was packed (pseudo/fakeroot records these for the image).
    chown -R root:root ${D}${SAMPLE_DEST_DIR}
    find ${D}${SAMPLE_DEST_DIR} -type d -exec chmod 0755 {} +
    find ${D}${SAMPLE_DEST_DIR} -type f -exec chmod 0644 {} +

    # ... except helper scripts, which stay executable.
    find ${D}${SAMPLE_DEST_DIR} -type f -name '*.sh' -exec chmod 0755 {} +
}

# Packaging Configuration
# Everything installed lives under one directory, so a single FILES entry ships
# the whole bundle - no QA skips needed for plain data files.
FILES:${PN} = "${SAMPLE_DEST_DIR}"
