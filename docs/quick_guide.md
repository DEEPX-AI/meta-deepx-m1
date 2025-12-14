# Quick Start Guide: DEEPX M1 NPU Runtime Integration

This document serves as a step-by-step guide for integrating the **DEEPX M1 NPU Runtime Layer** into your Yocto Project environment using the official repository.

## 1. Layer Overview

The `meta-deepx-m1` layer provides the essential drivers, runtime libraries, and streaming components required to operate the **DX-M1** accelerator.

### Component Summary

| Component | Package Name | Description |
| :--- | :--- | :--- |
| **Driver** | `dx-driver` | Kernel module for the NPU device |
| **Runtime** | `dx-rt` | Userspace runtime API & libraries |
| **Stream** | `dx-stream` | Streaming pipeline framework |
| **External** | `libonnxruntime` | ONNX Runtime dependency (v1.20.1) |

## 2. Installation

### 2.1. Clone the Repository
Clone the `meta-deepx-m1` repository into your Yocto source directory (e.g., `sources/`).

```bash
# Go to your yocto sources directory
cd /path/to/yocto/sources/

# Clone the repository
git clone [https://github.com/DEEPX-AI/meta-deepx-m1.git](https://github.com/DEEPX-AI/meta-deepx-m1.git)
````

> **Note:** If you are using a specific Yocto release (e.g., Scarthgap, Kirkstone), please check out the corresponding branch after cloning:
>
> ```bash
> cd meta-deepx-m1
> git checkout <branch-name>  # e.g., git checkout scarthgap
> ```

### 2.2. Register the Layer

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

## 3\. Configuration

To deploy the NPU components onto the target root filesystem, you must include them in your image configuration.

Open `conf/local.conf` (or your specific image recipe) and append the following lines:

```bitbake
# -------------------------------------------
# DEEPX M1 NPU Configuration
# -------------------------------------------

# Install Driver, Runtime, and Streamer
IMAGE_INSTALL:append = " dx-driver dx-rt dx-stream"

# (Optional) Install debugging tools if available
# IMAGE_INSTALL:append = " dx-tools"
```

> **⚠️ Important Note on Dependencies:**
> This layer includes `libonnxruntime` (v1.20.1). If your project uses another layer (e.g., `meta-oe`) that provides a different version of ONNX Runtime, please ensure `meta-deepx-m1` has a **higher priority** in `conf/layer.conf` to utilize the tested version provided here.

## 4\. Build

Build your target image using `bitbake`.

```bash
# Example for a standard minimal image
bitbake core-image-minimal
```

## 5\. Verification

Once the image is built and flashed onto the target device, verify the installation using the following steps.

### 5.1. Check Kernel Driver

Confirm that the NPU kernel module is loaded.

```bash
lsmod | grep dx
# Expected Output: dx_driver    <size>  0
```

*If the module is not loaded automatically, try running `modprobe dx_driver`.*

### 5.2. Check Libraries

Verify that the shared libraries are present in the system path (`/usr/lib` or `/usr/lib64`).

```bash
ls -l /usr/lib/libdx_rt.so*
ls -l /usr/lib/libdx_stream.so*
```

### 5.3. Check Installed Versions

If your image includes a package manager, you can query the installed package versions.

```bash
# For opkg users
opkg list-installed | grep dx-

# Expected Output (Example):
# dx-driver - 1.8.0
# dx-rt - 3.1.0
# dx-stream - 2.1.0
```

-----

**Support & Troubleshooting**
For issues related to the repository or build failures, please check the [Issues](https://www.google.com/search?q=https://github.com/DEEPX-AI/meta-deepx-m1/issues) tab on the GitHub repository.
