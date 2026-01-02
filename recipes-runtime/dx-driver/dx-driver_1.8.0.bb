SUMMARY = "DEEPX NPU Driver"
DESCRIPTION = "DEEPX M1 NPU Linux Kernel Drivers"
HOMEPAGE = "https://deepx.ai"
LICENSE = "Proprietary"
LIC_FILES_CHKSUM = "file://../LICENSE;md5=df0ebe3edba67d21cb2e798ef0ee2905"

inherit module

PROVIDES:${PN} = "kernel-module-${PN}"

# Use gitsm:// fetcher to automatically clone with submodules (--recurse-submodules)
# This is equivalent to: git clone --recurse-submodules git@github.com:DEEPX-AI/dx_rt_npu_linux_driver.git
SRC_URI = "gitsm://github.com/DEEPX-AI/dx_rt_npu_linux_driver.git;protocol=https;branch=main"
SRCREV = "a90cd9616f6ebe1e10feafb1371e4ca11f0c2c48"
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
