#!/usr/bin/env bash
# Build the LWJGL 3.3.3 Yoga JNI library from local inputs on Linux x86_64 (including WSL).
# LWJGL source archive:
#   https://codeload.github.com/LWJGL/lwjgl3/tar.gz/f0ec1437a6149dae00eaa27c47268e5e4efb129f
# Android NDK r27d:
#   https://dl.google.com/android/repository/android-ndk-r27d-linux.zip
#   Published ZIP SHA-1: 22105e410cf29afcf163760cc95522b9fb981121
# The Android ABI and API level match the Amethyst 1.1.7 LWJGL build.
set -euo pipefail
export LC_ALL=C

if (( $# != 4 )); then
    echo "Usage: bash $0 <lwjgl-source.tar.gz> <ndk-r27d-directory> <arm64-v8a|x86_64> <output-directory>" >&2
    exit 2
fi

source_archive=$1
ndk_dir=$2
abi=$3
out_dir=$4

case "$abi" in
    arm64-v8a) target=aarch64-linux-android; lwjgl_arch=arm64 ;;
    x86_64) target=x86_64-linux-android; lwjgl_arch=x64 ;;
    *) echo "Unsupported Android ABI: $abi" >&2; exit 2 ;;
esac

expected_source_hash=388f5e64c708486cd6693adf40bb0bb01a51b83026298ad19eb764fa6f460784
actual_source_hash=$(sha256sum < "$source_archive")
if [[ ${actual_source_hash%% *} != "$expected_source_hash" ]]; then
    echo "Expected the LWJGL 3.3.3 archive for f0ec1437a6149dae00eaa27c47268e5e4efb129f" >&2
    exit 1
fi

ndk_bin="$ndk_dir/toolchains/llvm/prebuilt/linux-x86_64/bin"
compiler="$ndk_bin/${target}21-clang++"
if ! grep -qx 'Pkg.Revision = 27.3.13750724' "$ndk_dir/source.properties" ||
    [[ ! -x "$ndk_bin/clang++" || ! -x "$compiler" || ! -x "$ndk_bin/llvm-strip" ]]; then
    echo "Expected a complete Linux x86_64 Android NDK r27d (27.3.13750724) at $ndk_dir" >&2
    exit 1
fi

workspace=$(mktemp -d "${TMPDIR:-/tmp}/atomlib-yoga.XXXXXXXX")
trap 'rm -rf -- "$workspace"' EXIT
tar -xzf "$source_archive" --strip-components=1 -C "$workspace"

module_root="$workspace/modules/lwjgl"
yoga="$module_root/yoga/src"
core="$module_root/core/src/main/c"
version_script="$workspace/config/linux/version.script"
shopt -s globstar nullglob
yoga_sources=("$yoga"/main/c/yoga/**/*.cpp)
bridge="$yoga/generated/c/org_lwjgl_util_yoga_Yoga.cpp"
library="$workspace/liblwjgl_yoga.so"

# The Java binding loads this library alongside the launcher's existing LWJGL core.
# Statically link the C++ runtime and use 16 KiB ELF load segment alignment.
"$compiler" -std=gnu++14 -O3 -flto -fPIC -pthread \
    -DNDEBUG -DLWJGL_LINUX "-DLWJGL_$lwjgl_arch" \
    -I"$core" -I"$core/linux" -I"$yoga/main/c" \
    "${yoga_sources[@]}" "$bridge" \
    -shared -static-libstdc++ \
    "-Wl,--version-script=$version_script" \
    -Wl,--no-undefined,-soname,liblwjgl_yoga.so,-z,noexecstack \
    -Wl,-z,max-page-size=16384,-z,common-page-size=16384 \
    -o "$library"
"$ndk_bin/llvm-strip" --strip-unneeded "$library"

mkdir -p "$out_dir/$abi"
install -m 0644 "$library" "$out_dir/$abi/liblwjgl_yoga.so"
sha256sum "$out_dir/$abi/liblwjgl_yoga.so"
