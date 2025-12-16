# Quick Start Guide: DEEPX M1 NPU Integration

## 1. Introduction

The `meta-deepx-m1` Yocto layer streamlines the integration of DeepX M1 software into existing embedded Linux build systems. 

By embedding DeepX components directly into the Yocto workflow, this layer eliminates the need for manual makefile execution on target devices and removes concerns about build-time dependencies. This approach ensures that the DeepX software stack is:

* **Seamlessly Integrated:** Aligned with the complete system image.  
* **Fully Reproducible:** Consistent builds across different environments.  
* **Production-Ready:** Ideal for scalable deployment without disrupting existing Yocto workflows.  

This guide provides a reliable path for deploying DeepX hardware acceleration capabilities directly into your system image.

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

## 3. Installation

### 3.1. Clone the Repository
Clone the `meta-deepx-m1` repository into your Yocto source directory (e.g., `sources/`).

```bash
# Go to your yocto sources directory
cd /path/to/yocto/sources/

# Clone the repository
git clone -b scarthgap https://github.com/DEEPX-AI/meta-deepx-m1.git
```

### 3.2. Register the Layer

Add the layer path to your build configuration file (`conf/bblayers.conf`).

**Option A: Using bitbake-layers (Recommended)**  
Execute the following command from your build directory:

```bash
bitbake-layers add-layer ../sources/meta-deepx-m1
```

**Option B: Manual Edit**  
Open `conf/bblayers.conf` and append the full path:

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

# Install Driver, Runtime, and Streamer
IMAGE_INSTALL:append = " dx-driver dx-rt dx-stream"

```

> **⚠️ Important Note on Dependencies:**  
> This layer includes `libonnxruntime` (v1.20.1). If your project uses another layer (e.g., `meta-oe`) that provides a different version of ONNX Runtime, please ensure `meta-deepx-m1` has a **higher priority** in `conf/layer.conf` to utilize the tested version provided here.

## 5. Build

Build your target image using `bitbake`.

```bash
# Example for a standard minimal image
bitbake core-image-minimal
```

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

*If the module is not loaded automatically, try running `modprobe dxrt_driver` (or `dx_driver` depending on your specific kernel module name).*

### 6.2. Check Libraries

Verify that the shared libraries are present in the system path (`/usr/lib` or `/usr/lib64`).

```bash
ls -l /usr/lib/libdxrt.so*
ls -l /usr/lib/gstreamer-1.0/libgstdxstream.so*
```

### 6.3. Check Installed Versions

The `dx-rt` package provides a command-line utility named `dxrt-cli` to inspect the system status. This is the recommended method to verify that all components (Runtime, Driver, Firmware, and Hardware) are correctly synchronized.

Run the status command:

```bash
dxrt-cli -s
```

**Expected Output:**  
You should see a status report similar to the following. Please verify that the **DX-RT** and **RT Driver version** match the target release numbers.  

```text
DX-RT v3.x.x
=======================================================
 * Device 0: M1, Accelerator type
-------------------    Version    ---------------------
...
=======================================================
```

> **Note:**  
> If `dxrt-cli` returns an error or fails to open the device, please ensure the kernel driver is loaded (Refer to Section 6.1).

To verify that the dx-stream plugin is correctly installed, execute the following command:

```bash
gst-inspect-1.0 dxstream | grep dx
```
**Expected Output:**  The command should display the plugin details (such as dxvideosink, dxvideodec) without returning a "no such element" error.

---
    