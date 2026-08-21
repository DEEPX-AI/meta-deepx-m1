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

---

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

# Install the YOLO26 applications and their assets (models, videos, images)
IMAGE_INSTALL:append = " dx-yolo26 dx-yolo26-sample"

# GStreamer 1.0 related options need to be added.

# include ncurses-term for dxtop
CORE_IMAGE_EXTRA_INSTALL += "ncurses-terminfo-base"
```

> **⚠️ Important Note on Dependencies:** > This layer includes `libonnxruntime` (v1.20.1). If your project uses another layer (e.g., `meta-oe`) that provides a different version of ONNX Runtime, please ensure `meta-deepx-m1` has a **higher priority** in `conf/layer.conf` to utilize the tested version provided here.

---

## 5. Build

Build your target image using `bitbake`.

```bash
# Example for weston image
bitbake core-image-weston
```

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

---

## 7. YOLO26 Applications

`dx-yolo26` installs five DXRT-based YOLO26 applications, and `dx-yolo26-sample`
installs everything they need to run: the models, a few video clips and still
images. Both are optional - skip them if the image only needs the runtime.

### 7.1. Installed Files

```text
/usr/bin/yolo26n_async, yolo26n_cls_async, yolo26n_pose_async,
         yolo26n_seg_async, yolo26n_depth_async
/usr/share/dx_yolo26/examples/<category>/<model>/config.json   # postprocess thresholds
/usr/share/dx-yolo26-sample/assets/models/*.dxnn               # models
/usr/share/dx-yolo26-sample/assets/videos/*                    # video clips
/usr/share/dx-yolo26-sample/sample/img/*                       # still images
```

### 7.2. Running an Application

Each application resolves its default model and its default input **relative to the
working directory**, so start it from the asset bundle and no arguments are needed:

```bash
cd /usr/share/dx-yolo26-sample

yolo26n_async                    # object detection
yolo26n_cls_async                # classification
yolo26n_pose_async               # pose estimation
yolo26n_seg_async                # instance segmentation
yolo26n_depth_async              # depth estimation
```

Rendering opens a window, so a compositor (for example weston) has to be running.
Over SSH or on a headless image, add `--no-display` to print the frame rate only:

```bash
yolo26n_async --no-display
```

Other inputs are selected explicitly:

```bash
# video clip, camera, RTSP stream
yolo26n_async -v assets/videos/snowboard.mp4
yolo26n_async -c 0
yolo26n_async -r rtsp://<host>:<port>/<path>

# an explicit model and image directory, results written to disk
yolo26n_seg_async -m assets/models/yolo26-n-seg_640x640.dxnn -i ./sample/img -s

# postprocess thresholds from the installed config
yolo26n_async --config /usr/share/dx_yolo26/examples/object_detection/yolo26n/config.json
```

### 7.3. Applications and Their Defaults

| Application | Task | Default model | Default image |
| :--- | :--- | :--- | :--- |
| `yolo26n_async` | Object detection | `yolo26-n-od_640x640.dxnn` | `sample_street.jpg` |
| `yolo26n_cls_async` | Classification | `yolo26-n-cls_224x224.dxnn` | `sample_dog.jpg` |
| `yolo26n_pose_async` | Pose estimation | `yolo26-n-pose_640x640.dxnn` | `sample_people.jpg` |
| `yolo26n_seg_async` | Instance segmentation | `yolo26-n-seg_640x640.dxnn` | `sample_street.jpg` |
| `yolo26n_depth_async` | Depth estimation | `yolo26-depth-n_768x768.dxnn` | `sample_parking.jpg` |

Paths are relative to the working directory: models under `assets/models/`, images
under `sample/img/`.

### 7.4. Common Options

* `-m, --model_path <file.dxnn>` - model to run; omitted, the application uses its default
* `-i, --image_path <file|dir>` - image file or a directory of images
* `-v, --video_path <file>` - video file (mp4, mov, avi)
* `-c, --camera_index <n>` - camera device index
* `-r, --rtsp_url <url>` - RTSP stream
* `--no-display` - do not open a window, print the frame rate only
* `-l, --loop <n>` - repeat the inference n times
* `-s, --save`, `--save-dir <dir>` - write the rendered output to disk
* `--config <config.json>` - postprocess thresholds
* `--show-log`, `-h, --help`

### 7.5. Notes

* Only the asynchronous variants are packaged. To get the `*_sync` applications as
  well, build with `DXYOLO26_VARIANTS:pn-dx-yolo26 = "both"` in `conf/local.conf`.
* Additional or larger YOLO26 models come from
  [DX-ModelZoo](https://developer.deepx.ai/modelzoo/). Put them under
  `assets/models/` next to the working directory, or pass the path with `-m`.
* The applications link OpenCV `highgui` and `videoio`; the OpenCV in your image has
  to provide those modules for display and video input to work.
