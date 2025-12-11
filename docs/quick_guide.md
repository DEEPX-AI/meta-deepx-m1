# 🚀 meta-deepx-m1 Yocto Layer Quick Guide

This document provides a brief overview of the structure and usage of the `meta-deepx-m1` layer, which supports the Yocto Linux image build for the DeepX M1 platform.

## 1. Layer Introduction

`meta-deepx-m1` is designed to integrate the DeepX M1 hardware accelerator, its associated runtime (DXRT), and demonstration applications (DXApps) into an embedded Linux system based on the Yocto Project.

## 2. Recipe Directory Structure

This layer manages recipes by grouping them based on functionality. The core directories are as follows:

| Directory Path | Functional Group | Example Recipes |
| :--- | :--- | :--- |
| `recipes-dxrt` | DeepX Runtime (DXRT) components | `dx-driver`, `dx-ort`, `dx-runtime` |
| `recipes-dxapps` | DeepX Applications (DXApps) | `dx-stream` |

## 3. Core Recipe Descriptions

| Recipe Name | Directory | Description |
| :--- | :--- | :--- |
| **`dx-driver`** | `recipes-dxrt` | Recipe for the **kernel driver** and user-space libraries for the M1 hardware accelerator. This is essential for hardware access. |
| **`dx-runtime`** | `recipes-dxrt` | Builds the core **Inference Runtime (DXRT)** libraries and related utilities. This is the main component responsible for executing neural network models. |
| **`dx-ort`** | `recipes-dxrt` | Recipe for **ONNX Runtime (ORT)** integration. It supports DXRT acting as an executor provider for ORT. |
| **`dx-stream`** | `recipes-dxapps` | Recipe for a **demonstration streaming application** that utilizes the DeepX platform (e.g., handling video streaming input and outputting inference results). |

## 4. Layer Usage Instructions

### 4.1. Setting up the Build Environment

To use the `meta-deepx-m1` layer, you must add it to the layer list in your `bitbake` build environment configuration files.

1.  **Modify `conf/bblayers.conf`:**
    Open the `conf/bblayers.conf` file inside your build directory and add the path to the layer:

    ```bash
    # Example bblayers.conf
    BBLAYERS += " \
      # ... existing layers
      /path/to/meta-deepx-m1 \
    "
    ```
    (Replace `/path/to/meta-deepx-m1` with the actual path to the layer.)

2.  **Configure `conf/local.conf` (Optional):**
    If you need to override specific recipe variables or apply additional settings (e.g., enabling certain DXRT features), modify the `conf/local.conf` file.

### 4.2. Building Specific Recipes

Once the layer is added, you can build individual recipes using the `bitbake` command.

* **Example: Building `dx-runtime`:**
    ```bash
    bitbake dx-runtime
    ```

* **Example: Building the `dx-stream` demo application:**
    ```bash
    bitbake dx-stream
    ```

### 4.3. Integrating into the Final Image

To include the built packages in your final target image, add the desired recipe package names to your image recipe (e.g., `core-image-sato`, `my-custom-image`).

* **Example:** Adding packages to an image recipe file (`*.bb`)
    ```python
    IMAGE_INSTALL += " \
        dx-driver \
        dx-runtime \
        dx-ort \
        dx-stream \
    "
    ```

---

I hope this guide helps clarify the structure and usage of your $\text{meta-deepx-m1}$ repository!

