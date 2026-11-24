SUMMARY = "DEEPX NPU Driver"
DESCRIPTION = "DEEPX M1 NPU Linux Kernel Drivers"
HOMEPAGE = "https://deepx.ai"
# The kernel modules are GPL-2.0: every source file carries
# "SPDX-License-Identifier: GPL-2.0" and the modules declare MODULE_LICENSE("GPL").
# At this SRCREV the repository has no GPL text of its own (its top-level LICENSE
# is still the DEEPX proprietary notice, corrected upstream by DXM1-2903), so the
# checksum covers the SPDX headers and the GPL-2.0 text shipped with OE-Core.
# TODO(DXM1-2903): once the upstream fix is merged, bump SRCREV and point
# LIC_FILES_CHKSUM at the repository's COPYING instead.
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = " \
    file://${COMMON_LICENSE_DIR}/GPL-2.0-only;md5=801f80980d171dd6425610833a22dbe6 \
    file://Kbuild;beginline=1;endline=1;md5=daad6f7f7a0a286391cd7773ccf79340 \
    file://rt/dxrt_drv.c;beginline=1;endline=1;md5=50d2ba0afecd20f74c12a4bdbcfcfe61 \
    file://pci_deepx/dx_sgdma.c;beginline=1;endline=1;md5=50d2ba0afecd20f74c12a4bdbcfcfe61 \
"

inherit module

PROVIDES:${PN} = "kernel-module-${PN}"

# Use gitsm:// fetcher to automatically clone with submodules (--recurse-submodules)
# This is equivalent to: git clone --recurse-submodules git@github.com:DEEPX-AI/dx_rt_npu_linux_driver.git
SRC_URI = "gitsm://github.com/DEEPX-AI/dx_rt_npu_linux_driver.git;protocol=https;branch=main"
SRCREV = "8455be625439e005d7b3a375cc79087f4b570404"
S = "${WORKDIR}/git/modules"

EXTRA_OEMAKE = "DEVICE=${DX_DEVICE} \
                PCIE=${DX_PCIE} \
                KERNEL_DIR=${STAGING_KERNEL_BUILDDIR} \
                ARCH=${ARCH} \
                CROSS_COMPILE=${TARGET_PREFIX}"

DX_DEVICE ?= "m1"
DX_PCIE ?= "deepx"

# Install kernel modules and configuration
do_install() {
    # Install kernel modules using standard modules_install
    # DEPMOD=echo prevents running depmod during build (runs on first boot instead)
    oe_runmake DEPMOD=echo MODLIB="${D}${nonarch_base_libdir}/modules/${KERNEL_VERSION}" \
               INSTALL_FW_PATH="${D}${nonarch_base_libdir}/firmware" \
               DEVICE=${DX_DEVICE} \
               KERNEL_DIR=${STAGING_KERNEL_BUILDDIR} \
               ARCH=${ARCH} \
               CROSS_COMPILE=${TARGET_PREFIX} \
               INSTALL_DIR=${D} \
               CC="${KERNEL_CC}" LD="${KERNEL_LD}" \
               install
    
    # Install modprobe configuration
    install -d ${D}${sysconfdir}/modprobe.d
    if [ -f ${S}/dx_dma.conf ]; then
        install -m 0644 ${S}/dx_dma.conf ${D}${sysconfdir}/modprobe.d/
    fi

    # Create and install udev rules for device node access permissions
    # This rule sets the mode of /dev/dxrt* to 0666 (read/write for everyone)
    install -d ${D}${sysconfdir}/udev/rules.d
    echo 'KERNEL=="dxrt*", MODE="0666"' > ${D}${sysconfdir}/udev/rules.d/99-dx-dma.rules
}

# Explicitly declare files to be packaged
FILES:${PN} += "${nonarch_base_libdir}/modules/${KERNEL_VERSION}/extra/*.ko"
FILES:${PN} += "${sysconfdir}/modprobe.d/dx_dma.conf"

# Add udev rules file to the package
FILES:${PN} += "${sysconfdir}/udev/rules.d/99-dx-dma.rules"
