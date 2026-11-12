# ROPC — Cordial ARM64 launcher (experimental)

ROPC is a native Android controller for launching **Cordial** inside Termux + Debian ARM64. It does not draw a fake Windows desktop and is not itself an emulator.

## Target device

The current test target is a Samsung Galaxy A16 5G (Exynos 1330 / Mali-G68), Android 16, ARM64, 4 KB memory pages. The device's Termux environment has already reported the physical Mali-G68 Vulkan device. Vulkan access from inside Debian/proot still needs to be confirmed.

## What the APK does

- Shows whether Termux and Termux:X11 are installed.
- Uses Termux's `RUN_COMMAND` service to run a setup script.
- Installs `proot-distro` and a Debian ARM64 root filesystem.
- Downloads the official Cordial 0.34.1 ARM64 Debian package and an Android Vulkan wrapper package.
- Verifies both downloads against pinned SHA-256 values before installing them.
- Starts Termux:X11 and launches Cordial after checking that Debian can see the Mali-G68 Vulkan device.

## Required apps and one-time permission

1. Install Termux from the official project: https://github.com/termux/termux-app/releases
2. Install the Termux:X11 Android companion APK: https://github.com/termux/termux-x11/releases
3. In Termux, edit `~/.termux/termux.properties` and set:
   `allow-external-apps=true`
4. Run `termux-reload-settings`.
5. In Android Settings → Apps → ROPC → Permissions, allow the Termux command permission if Android presents it.

The APK's **Preparar Cordial + Debian** button runs `scripts/ropc-cordial.sh setup`. The **Iniciar Roblox** button runs `scripts/ropc-cordial.sh run`.

For manual setup from Termux, download the script from this repository and run:

```sh
curl -fL https://raw.githubusercontent.com/Tolezack/ropc/main/scripts/ropc-cordial.sh -o ~/ropc-cordial.sh
chmod +x ~/ropc-cordial.sh
bash ~/ropc-cordial.sh setup
bash ~/ropc-cordial.sh run
```

The initial Debian and Cordial downloads need a stable internet connection and several GB of free storage are recommended. On first Cordial launch, press **Download Roblox**; Cordial downloads the Android client and verifies Roblox's signing certificate.

## GPU handoff

The setup installs the ARM64 `vulkan-wrapper-android` package inside Debian and selects its ICD JSON when present. The run script calls `vulkaninfo --summary` inside Debian and refuses to launch if it does not see `Mali-G68`, so it does not silently fall back to software rendering.

If this check fails, the wrapper/driver bridge is not working in the guest yet. It does **not** mean the phone's Android Vulkan driver is missing: the Termux-side test and the Debian/proot-side test are separate.

## Important limits

- Cordial runs Roblox's **official Android build** natively on Linux ARM64. It does not run the Windows Roblox Player.
- Some experiences can still show a mobile/touch interface or reject Android clients. This does not guarantee access to experiences that require the Windows/PC client.
- Cordial's ARM64 packages are published, but the Cordial project says real ARM64 phones have not yet been tested. This ROPC integration is experimental.
- Cordial currently targets Wayland; X11 is supported but rougher. The ROPC script explicitly selects X11 because the current phone setup uses Termux:X11.
- Cordial 0.34.1 notes that Android ImageReader video decoding is unsupported in that release.
- Do not use this project to modify Roblox, impersonate Windows, or bypass anti-cheat.

## Sources

- Cordial: https://github.com/luohoa97/cordial
- Cordial installation guide: https://github.com/luohoa97/cordial/blob/main/docs/install.md
- Cordial ARM64 status: https://github.com/luohoa97/cordial/blob/main/docs/multiarch.md
- Termux:X11: https://github.com/termux/termux-x11

## Build the APK

Requires JDK 17 and Gradle 8.9:

```sh
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds the debug APK on pushes to `main`.
