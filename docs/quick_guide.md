# Quick Start Guide: DEEPX M1 NPU Integration

## 1. Introduction

The `meta-deepx-m1` Yocto layer streamlines the integration of DeepX M1 software into existing embedded Linux build systems.

By embedding DeepX components directly into the Yocto workflow, this layer eliminates the need for manual makefile execution on target devices and removes concerns about build-time dependencies. This approach ensures that the DeepX software stack is:

* **Seamlessly Integrated:** Aligned with the complete system image.
* **Fully Reproducible:** Consistent builds across different environments.
* **Production-Ready:** Ideal for scalable deployment without disrupting existing Yocto workflows.

This guide provides a reliable path for deploying DeepX hardware acceleration capabilities directly into your system image.

---

## 2. Prerequisites

Before integrating the `meta-deepx-m1` layer, ensure the following prerequisites are satisfied to guarantee a smooth setup process:

### 2.1. Yocto Project Environment

A functional Yocto Project environment is required. This typically includes:

* The **Poky** build system.
* A valid machine configuration provided by your hardware vendor (BSP).
* *Note: Most hardware vendors provide a customized Yocto build environment that includes board support packages and integration scripts.*

### 2.2. Initialized Build Directory

The build directory (e.g., `build/`) must be initialized using the scripts provided by your vendor’s BSP or development kit. It must contain valid configuration files:

* `conf/local.conf`
* `conf/bblayers.conf`

### 2.3. System Resources

Building DeepX-enabled images can demand significant compute resources.

* **RAM:** Minimum 16 GB recommended.
* **Storage:** At least 100 GB of available disk space.

### 2.4. Skill Requirements

Developers should have working knowledge of core Yocto operations, including:

* Invoking builds via `bitbake`.
* Editing configuration files (`local.conf`).
* Managing layers using `bitbake-layers`.

### 2.5. Required Layers

Besides OE-Core and your board's BSP layer, `conf/bblayers.conf` must contain these `meta-openembedded` layers:

* `meta-oe` (`opencv`, `librdkafka`, `libeigen`)
* `meta-python` (`python3-pybind11`)
* `meta-networking` (`mosquitto`)

---

## 3. Installation

All commands in sections 3 to 5 run on the build host as a normal user (no `sudo`). `bitbake` and `bitbake-layers` are available after the build environment is initialized, for example `source poky/oe-init-build-env build`, which also changes into the build directory.

### 3.1. Clone the Repository

Clone the `meta-deepx-m1` repository into your Yocto source directory (e.g., `sources/`).

```bash
# Go to your yocto sources directory
cd /path/to/yocto/sources/

# Clone the repository
git clone -b walnascar https://github.com/DEEPX-AI/meta-deepx-m1.git
```

### 3.2. Register the Layer

Add the layer path to your build configuration file (`conf/bblayers.conf`).

**Option A: Using bitbake-layers (Recommended)** Execute the following command from your build directory:

```bash
bitbake-layers add-layer ../sources/meta-deepx-m1
```

**Option B: Manual Edit** Open `conf/bblayers.conf` and append the full path:

```bitbake
BBLAYERS ?= " \
  /home/user/yocto/sources/poky/meta \
  /home/user/yocto/sources/poky/meta-poky \
  /home/user/yocto/sources/meta-deepx-m1 \
  "
```

## 4. Configuration

To deploy the NPU components onto the target root filesystem, you must include them in your image configuration.

Open `conf/local.conf` (or your specific image recipe) and append the following lines:

```bitbake
# -------------------------------------------
# DEEPX M1 NPU Configuration
# -------------------------------------------

# Install Driver, Runtime, Streamer, and Streamer's sample
IMAGE_INSTALL:append = " dx-driver dx-rt dx-stream dx-stream-sample"
IMAGE_INSTALL:append = " libonnxruntime libyuv"

# GStreamer 1.0 related options need to be added.

# include ncurses-term for dxtop
CORE_IMAGE_EXTRA_INSTALL += "ncurses-terminfo-base"
```

> **⚠️ Important Note on Dependencies:** > This layer includes `libonnxruntime` (v1.20.1). If your project uses another layer (e.g., `meta-oe`) that provides a different version of ONNX Runtime, please ensure `meta-deepx-m1` has a **higher priority** in `conf/layer.conf` to utilize the tested version provided here.

---

## 5. Build

Build your target image using `bitbake`, from the build directory.

```bash
# Example for weston image
bitbake core-image-weston
```

A successful build ends with `Tasks Summary: ... all succeeded.`

### 5.1. Build Output

The image files are written to `tmp/deploy/images/<MACHINE>/` in the build directory, named after the image and the machine:

```text
build/tmp/deploy/images/<MACHINE>/
  <image>-<MACHINE>.rootfs.wic.gz     # disk image
  <image>-<MACHINE>.rootfs.wic.bmap   # block map for bmaptool
```

Which image types are produced (`wic.gz`, `wic.xz`, `tar.gz`, ...) depends on the `IMAGE_FSTYPES` of your BSP and `conf/local.conf`. Write the image to the boot medium as described in your board's BSP documentation.

### 5.2. If the Build Fails

| Symptom | What to do |
| :--- | :--- |
| `Nothing PROVIDES 'opencv'` (or `mosquitto`, `librdkafka`, `python3-pybind11`) | Add the layers listed in [2.5](#25-required-layers) to `conf/bblayers.conf`. |
| `do_fetch` fails for a `dx-*` recipe | Check that the build host can reach `github.com`. |
| `LIC_FILES_CHKSUM` mismatch | The recipe and the fetched source are out of sync. Use an unmodified checkout of this layer's branch. |

---

## 6. Verification

Once the image is built and flashed onto the target device, verify the installation using the following steps.

### 6.1. Check Kernel Driver

Confirm that the NPU kernel module is loaded.

```bash
lsmod | grep dx
# Expected Output:
# dxrt_driver    <size>  0
# dx_dma         <size>  1  dxrt_driver
```

### 6.2. Check Libraries

Verify that the shared libraries are present in the system path.

```bash
ls -l /usr/lib/libdxrt.so*
ls -l /usr/lib/gstreamer-1.0/libgstdxstream.so*
```

If `lsmod` in 6.1 lists nothing, check that `dx-driver` is in `IMAGE_INSTALL` and look at `dmesg | grep -i dx` for the device probe messages.

### 6.3. Check Installed Versions

Use the `dxrt-cli` utility to inspect the system status and version synchronization.


```bash
dxrt-cli -s
```

**Expected Output:**  
You should see a status report similar to the following. Please verify that the **DX-RT** and **RT Driver version** match the target release numbers.  

```text
DX-RT v3.x.x
...
```
