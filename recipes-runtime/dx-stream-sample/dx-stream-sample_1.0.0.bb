SUMMARY = "DeepX M1 Streaming Sample"
DESCRIPTION = "Recipe to install sample files with forced root ownership"
SECTION = "examples"
LICENSE = "CLOSED"

SRC_URI = "file://dx-stream-sample.tar.gz;unpack=0"

S = "${WORKDIR}"

# Defined installation path
SAMPLE_DEST_DIR = "/etc/dx-stream-sample"

RDEPENDS:${PN} += "bash"

do_install() {
    # 1. Create the target directory
    install -d ${D}${SAMPLE_DEST_DIR}

    # 2. Extract with flags to discard host UID/GID information
    # --no-same-owner: Don't try to preserve the UID/GID from the tarball
    # --no-same-permissions: Use default umask for permissions
    tar --no-same-owner --no-same-permissions -xf ${WORKDIR}/dx-stream-sample.tar.gz -C ${D}${SAMPLE_DEST_DIR} --strip-components=1

    # 3. Explicitly force root ownership just to be safe (Pseudo/Fakeroot handles this)
    chown -R root:root ${D}${SAMPLE_DEST_DIR}

    # 4. Set execution permissions for the script
    if [ -f ${D}${SAMPLE_DEST_DIR}/run.sh ]; then
        chmod +x ${D}${SAMPLE_DEST_DIR}/run.sh
    fi
}

FILES:${PN} = " \
    ${SAMPLE_DEST_DIR} \
    ${SAMPLE_DEST_DIR}/* \
"

INSANE_SKIP:${PN} += "installed-vs-shipped file-rdeps"