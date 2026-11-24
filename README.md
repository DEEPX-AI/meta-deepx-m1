# meta-deepx-m1 Yocto Layer

Yocto layer that adds DEEPX DX-M1 NPU support to an embedded Linux image: the kernel driver, the DX-RT runtime, the DX-Stream GStreamer plugin and demo applications. It is a distribution layer for Yocto users; the SDK sources themselves live in their own repositories (`dx_rt`, `dx_rt_npu_linux_driver`, `dx_stream`) and are fetched by the recipes.

| | |
| :--- | :--- |
| **Yocto release** | `walnascar` (this branch). Use the branch that matches your Yocto release. |
| **Target** | 64-bit Arm (`aarch64`) boards with a DX-M1 module |
| **Component set** | dx-driver 2.6.0, dx-rt 3.4.1, dx-stream 3.1.1, libonnxruntime 1.20.1 |
| **Start here** | [Quickstart](#quickstart) below, then the [Quick Guide](docs/quick_guide.md) for details and on-target checks |
| **Licenses** | Per recipe, see [Licenses](#licenses) |

## Quickstart

Goal: a bootable image that contains the DX-M1 driver and runtime.

**Before you start**

* A Linux build host with a Yocto `walnascar` environment that already builds an image for your board (set up from your board vendor's BSP). About 16 GB RAM and 100 GB free disk.
* These layers in `conf/bblayers.conf`: `meta-oe`, `meta-python`, `meta-networking` (from `meta-openembedded`) and your board's BSP layer.
* Internet access from the build host (sources are fetched from GitHub). No `sudo` is needed; do not run `bitbake` as root.

**Steps** (on the build host; `<yocto-dir>` is the directory that contains `poky/`)

```bash
cd <yocto-dir>
git clone -b walnascar https://github.com/DEEPX-AI/meta-deepx-m1.git

source poky/oe-init-build-env build      # leaves you in <yocto-dir>/build
bitbake-layers add-layer ../meta-deepx-m1

cat >> conf/local.conf <<'EOF'
IMAGE_INSTALL:append = " dx-driver dx-rt dx-stream libonnxruntime libyuv"
CORE_IMAGE_EXTRA_INSTALL += "ncurses-terminfo-base"
EOF

bitbake core-image-weston
```

**Expected result**

* `bitbake` ends with `Tasks Summary: ... all succeeded.`
* The image is in `<yocto-dir>/build/tmp/deploy/images/<MACHINE>/`. For `core-image-weston`: `core-image-weston-<MACHINE>.rootfs.wic.gz` and `core-image-weston-<MACHINE>.rootfs.wic.bmap` (the compression suffix follows your `IMAGE_FSTYPES`).
* After writing the image and booting the board, on the target:

  ```bash
  lsmod | grep dx      # dxrt_driver and dx_dma are listed
  dxrt-cli -s          # prints the DX-RT version and the device status
  ```

**If it fails**

| Symptom | What to do |
| :--- | :--- |
| `Nothing PROVIDES 'opencv'` (or `mosquitto`, `librdkafka`, `python3-pybind11`) | A dependency layer is missing. Add `meta-oe`, `meta-networking`, `meta-python` to `conf/bblayers.conf`. |
| `do_fetch` fails for a `dx-*` recipe | Check that the build host can reach `github.com`. |
| `lsmod` shows no `dx` module on the target | Check that `dx-driver` is in `IMAGE_INSTALL` and that the DX-M1 module is detected (`dmesg \| grep -i dx`). |

The demo package (`dx-stream-sample`), GStreamer settings and all on-target checks are described in the [Quick Guide](docs/quick_guide.md).

## Recipes

| Recipe | Contents |
| :--- | :--- |
| `dx-driver` | DX-M1 kernel modules (`dxrt_driver`, `dx_dma`), modprobe and udev configuration |
| `dx-rt` | DX-RT runtime library, `dxrtd` service, CLI tools (`dxrt-cli`, `run_model`, `dxtop`), Python module |
| `dx-stream` | DX-Stream GStreamer plugin and postprocess libraries |
| `dx-stream-sample` | Demo assets: model, video clip, launcher script |
| `libonnxruntime` | Prebuilt ONNX Runtime 1.20.1 for `aarch64`, used by DX-RT |
| `libyuv` | YUV conversion library, used by DX-Stream |

## Layer Dependencies

| Layer | Needed for |
| :--- | :--- |
| `poky/meta` (OE-Core) | Base system, GStreamer |
| `meta-openembedded/meta-oe` | `opencv`, `librdkafka`, `libeigen` |
| `meta-openembedded/meta-python` | `python3-pybind11` |
| `meta-openembedded/meta-networking` | `mosquitto` |
| Board BSP layer | Kernel and machine configuration of your board |

## Licenses

The `LICENSE` field of each recipe is what ends up in your image's license manifest (`tmp/deploy/licenses/`).

| Recipe | `LICENSE` | Notes |
| :--- | :--- | :--- |
| `dx-driver` | `GPL-2.0-only` | Kernel modules. Source must be offered with devices you ship. |
| `dx-rt` | `Proprietary & MIT & BSD-3-Clause` | DEEPX runtime plus third-party code listed in `NOTICE.md` |
| `dx-stream` | `Proprietary` | |
| `libonnxruntime` | `MIT` | Upstream `LICENSE` and `ThirdPartyNotices.txt` |
| `libyuv` | `BSD-3-Clause` | |
| `dx-stream-sample` | `CLOSED` | Demo assets. Their redistribution terms are not declared by this layer; confirm them with DEEPX before putting them in a product image. |

License and notice files are installed on the target under `/usr/share/licenses/<package>/` for `dx-rt`, `dx-stream` and `libonnxruntime`.

### Recommended settings for device makers

If you ship devices built with this layer, these `conf/local.conf` settings help you meet notice and source-offer obligations for the whole image:

```bitbake
# Archive the patched sources of every recipe and generate an SPDX SBOM
INHERIT += "archiver create-spdx"
ARCHIVER_MODE[src] = "patched"

# Put the license manifest and the license texts into the image
COPY_LIC_MANIFEST = "1"
COPY_LIC_DIRS = "1"

# Only for devices with Secure Boot / locked-down firmware:
# INCOMPATIBLE_LICENSE = "GPL-3.0* LGPL-3.0* AGPL-3.0*"
```

After a build, check `tmp/deploy/licenses/<image>/license.manifest` and keep the archived sources from `tmp/deploy/sources/` together with the release.

## Documentation

* [Quick Guide](docs/quick_guide.md): prerequisites, configuration, build, on-target verification
