# ROPC Runner

ROPC Runner is a small native Android launcher. It does **not** display a pretend Windows desktop and does not claim to be a full PC emulator.

## Current behavior

- Opens Android's real document picker.
- Copies selected programs to `Download/ROPC`.
- `.EXE` / `.MSI`: detects Winlator and opens its app. The file is copied to shared storage so the user can select it inside Winlator and create a shortcut.
- `.SH` / Linux ELF: can hand off the command to Termux using its documented `RUN_COMMAND` service, targeting a Debian container managed by `proot-distro`.
- Supports physical keyboard shortcut `Ctrl+O` to select a file.
- Android APK is built for `arm64-v8a` and `x86_64`.

## Windows programs

Install Winlator from its official release page:

https://github.com/brunodev85/winlator/releases

Winlator uses Wine and Box86/Box64 to run Windows applications. ROPC copies the executable into `Download/ROPC` and opens Winlator. Since this project does not use a documented public Winlator API for launching an arbitrary EXE from another app, the user must select the file inside Winlator and create a shortcut. The expected Wine path is:

`Z:\storage\emulated\0\Download\ROPC\<filename>`

Compatibility is not guaranteed for every Windows program. This project does not modify Roblox, spoof a Windows device, or bypass anti-cheat.

## Linux programs

Install Termux from the official project:

https://github.com/termux/termux-app/releases

In Termux, install a Debian root filesystem:

```sh
pkg update
pkg install proot-distro
proot-distro install debian
termux-setup-storage
```

Then allow external commands:
1. In Termux, edit `~/.termux/termux.properties`.
2. Set `allow-external-apps=true`.
3. Run `termux-reload-settings`.
4. In Android Settings → Apps → ROPC Runner → Permissions, grant the additional permission to run commands in Termux, if Android shows it.

Termux must also be allowed to read shared storage. ROPC copies the file to:

`/storage/emulated/0/Download/ROPC`

The Linux binary must match the Debian container architecture, or the user must separately configure an appropriate QEMU user-mode emulator for cross-architecture execution. Linux GUI programs may need a graphical server such as Termux:X11; the current handoff is for terminal programs and scripts.

## Important limitations

- Android does not natively run Windows PE executables or generic GNU/Linux ELF binaries as desktop programs.
- ROPC does not bundle Wine, Box64, QEMU, a Debian root filesystem, or a Windows environment.
- Winlator and Termux are separate runtime apps that must be installed and configured by the user.
- The Winlator path/shortcut step is manual because no supported public external-launch API is used here.
- Selecting and executing untrusted programs can damage data. Only run files you trust.

## Build

Requires JDK 17 and Gradle 8.9:

```sh
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

GitHub Actions builds the APK on pushes to `main`.
