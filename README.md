# meta-deepx-m1 Yocto Layer

This layer provides recipes for integrating the DeepX M1 hardware accelerator, its runtime (DXRT), and associated applications into Yocto Project-based Linux distributions.

---

## 1. Overview and Purpose

The `meta-deepx-m1` layer is essential for building embedded Linux images that target the DeepX M1 platform. It encapsulates all necessary software components, including low-level kernel modules, core inference libraries, and demonstration applications.

Key responsibilities include:

* Providing M1-specific **kernel drivers** (`dx-driver`).
* Integrating the DeepX **Runtime (DXRT)** libraries (`dx-rt`).
* Enabling **ONNX Runtime (ORT)** execution through DXRT.
* Packaging DeepX **Applications** (e.g., streaming demos).

## 2. Dependencies

This layer depends on several core Yocto layers. Ensure these layers are included in your `conf/bblayers.conf` file before attempting to use `meta-deepx-m1`.

| Layer Name | URL/Source | Notes |
| :--- | :--- | :--- |
| `poky/meta` | (Included by default) | Core metadata layer |
| `meta-openembedded` | (e.g., `meta-oe`, `meta-python`) | Required for common utilities and Python support |
| `meta-yocto-bsp` | (Target specific BSP) | The specific Board Support Package (BSP) layer for the M1 target hardware |

## 3. Setup and Usage

### 3.1. Cloning the Layer

Clone this repository into your Yocto build environment's source directory:

```bash
cd <your-yocto-build-dir>
git clone -b scarthgap https://github.com/DEEPX-AI/meta-deepx-m1.git
```

## 4. Documentation  

For more detailed setup instructions and examples, please refer to the following documentation:  

* [Quick Guide](https://github.com/DEEPX-AI/meta-deepx-m1/blob/scarthgap/docs/quick_guide.md)

