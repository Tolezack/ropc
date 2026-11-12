#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

MODE="\${1:-help}"
APP_HOME="$HOME/.local/share/ropc-cordial"
CACHE="$APP_HOME/cache"
DEBIAN_ROOT="$PREFIX/var/lib/proot-distro/installed-rootfs/debian"
CORDIAL_DEB="cordial_0.34.1-1_arm64.deb"
WRAPPER_DEB="vulkan-wrapper-android_25.0.0-3_aarch64.deb"
CORDIAL_URL="https://github.com/luohoa97/cordial/releases/download/v0.34.1/$CORDIAL_DEB"
CORDIAL_SHA="f3097af95cfb58a043f5aca72e4b45a694963efce20e9fe769c6427d2ba7e435"
WRAPPER_URL="https://github.com/sabamdarif/termux-desktop/releases/download/vulkan-wrapper-android_25.0.0-3/$WRAPPER_DEB"
WRAPPER_SHA="fc11784fee6a553eae78b30a4bff267981b735be7b1d4fac4e894e09be8058ca"
DEBIAN_BIND="--bind $CACHE:/mnt/ropc"

say() { printf '\n[ROPC] %s\n' "$*"; }
fail() { printf '\n[ROPC ERRO] %s\n' "$*" >&2; exit 1; }

check_host() {
    [ "$(uname -m)" = "aarch64" ] || fail "Este instalador é específico para ARM64. Arquitetura encontrada: $(uname -m)"
    command -v pkg >/dev/null 2>&1 || fail "Execute este script no Termux, não dentro do Debian."
}

download_checked() {
    local url="$1" file="$2" expected="$3"
    if [ ! -s "$file" ]; then
        say "Baixando $(basename "$file")..."
        curl -fL --retry 3 --retry-delay 2 "$url" -o "$file"
    fi
    echo "$expected  $file" | sha256sum -c - >/dev/null || {
        rm -f "$file"
        fail "SHA-256 inválido para $(basename "$file"). Download removido."
    }
}

host_setup() {
    check_host
    mkdir -p "$CACHE"
    say "Preparando pacotes do Termux..."
    pkg update -y
    pkg install -y proot-distro x11-repo termux-x11-nightly curl wget
    if [ ! -d "$DEBIAN_ROOT" ]; then
        say "Instalando Debian ARM64. O primeiro download pode ser grande..."
        proot-distro install debian
    fi
    [ -d "$DEBIAN_ROOT" ] || fail "O Debian não foi instalado. Verifique a saída do proot-distro."

    download_checked "$CORDIAL_URL" "$CACHE/$CORDIAL_DEB" "$CORDIAL_SHA"
    download_checked "$WRAPPER_URL" "$CACHE/$WRAPPER_DEB" "$WRAPPER_SHA"

    say "Instalando Cordial e o wrapper Vulkan dentro do Debian..."
    proot-distro login debian --shared-tmp $DEBIAN_BIND -- /bin/bash -lc '
        set -Eeuo pipefail
        dpkg --print-architecture | grep -qx arm64 || { echo "O Debian instalado não é ARM64."; exit 20; }
        apt-get update
        apt-get install -y ca-certificates curl wget file dbus-x11 vulkan-tools xterm
        apt-get install -y /mnt/ropc/'"$WRAPPER_DEB"' /mnt/ropc/'"$CORDIAL_DEB"'
        command -v cordial
        echo
        echo "Cordial instalado. O Roblox ainda será baixado pelo próprio Cordial na primeira abertura."
    '
    say "Instalação concluída. Use a opção Iniciar Roblox no ROPC."
}

start_x11() {
    check_host
    command -v termux-x11 >/dev/null 2>&1 || fail "Termux:X11 não está instalado. No Termux: pkg install x11-repo && pkg install termux-x11-nightly"
    if ! pgrep -f 'termux-x11 :1' >/dev/null 2>&1; then
        say "Iniciando servidor Termux:X11 :1..."
        (termux-x11 :1 -ac >/dev/null 2>&1 &) || true
        sleep 2
    fi
    if command -v am >/dev/null 2>&1; then
        am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity >/dev/null 2>&1 || true
    fi
    sleep 2
}

launch_cordial() {
    check_host
    [ -d "$DEBIAN_ROOT" ] || fail "Debian ainda não está instalado. Toque primeiro em Preparar Cordial."
    start_x11

    say "Verificando Vulkan dentro do Debian..."
    proot-distro login debian --shared-tmp $DEBIAN_BIND -- /bin/bash -lc '
        set -Eeuo pipefail
        export DISPLAY=:1
        export XDG_RUNTIME_DIR=/tmp
        export CORDIAL_X11=1
        ICD="$(find /usr/share/vulkan/icd.d -maxdepth 1 -type f \( -iname "*wrapper*.json" -o -iname "*android*.json" \) 2>/dev/null | head -n 1)"
        if [ -n "$ICD" ]; then
            export VK_ICD_FILENAMES="$ICD"
            echo "ICD Vulkan: $ICD"
        fi
        if ! command -v cordial >/dev/null 2>&1; then
            echo "Cordial não está instalado. Execute Preparar Cordial no ROPC."
            exit 21
        fi
        if ! vulkaninfo --summary 2>&1 | tee /tmp/ropc-vulkan-check.log; then
            echo
            echo "O Debian ainda não consegue criar um dispositivo Vulkan."
            echo "O Vulkan do Android foi detectado no Termux, mas falta confirmar o wrapper dentro do Debian."
            exit 22
        fi
        if ! grep -q "Mali-G68" /tmp/ropc-vulkan-check.log; then
            echo
            echo "AVISO: Mali-G68 não apareceu na enumeração Vulkan do Debian."
            echo "Não vou iniciar o jogo com um driver de software sem você saber."
            exit 23
        fi
        echo
        echo "Abrindo Cordial. Na primeira execução, escolha Download Roblox."
        if command -v dbus-run-session >/dev/null 2>&1; then
            exec dbus-run-session -- cordial
        else
            exec cordial
        fi
    '
}

case "$MODE" in
    setup|install) host_setup ;;
    run|start) launch_cordial ;;
    status)
        check_host
        printf 'Arquitetura: %s\n' "$(uname -m)"
        printf 'Android ABI: %s\n' "$(getprop ro.product.cpu.abi 2>/dev/null || echo desconhecida)"
        [ -d "$DEBIAN_ROOT" ] && echo "Debian: instalado" || echo "Debian: não instalado"
        [ -s "$CACHE/$CORDIAL_DEB" ] && echo "Pacote Cordial: baixado" || echo "Pacote Cordial: ainda não baixado"
        ;;
    *)
        echo "Uso: ropc-cordial.sh {setup|run|status}"
        ;;
esac
