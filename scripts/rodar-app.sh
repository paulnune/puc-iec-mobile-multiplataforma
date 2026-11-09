#!/usr/bin/env bash
# Roda, testa e compila o app eleicoes-kmp em qualquer Linux (nativo ou WSL 2). Ajuda: scripts/rodar-app.sh help
set -euo pipefail

REPO="$(cd "$(dirname "$0")/.." && pwd)"
APP="$REPO/exercicios/projeto-final/eleicoes-kmp"
AVD="${AVD:-eleicoes36}"
AVD_IMAGE="${AVD_IMAGE:-system-images;android-36;google_apis;x86_64}"
PKG="com.puciec.eleicoes"
LOG="${TMPDIR:-/tmp}/emulator-$AVD.log"

# ---------------------------------------------------------------- saída ---
if [ -t 1 ]; then G=$'\033[32m'; R=$'\033[31m'; Y=$'\033[33m'; N=$'\033[0m'; else G=""; R=""; Y=""; N=""; fi
ok()   { printf '  %s✔%s %s\n' "$G" "$N" "$*"; }
fail() { printf '  %s✘%s %s\n' "$R" "$N" "$*"; }
warn() { printf '  %s!%s %s\n' "$Y" "$N" "$*"; }
die()  { printf '%s✘ %s%s\n' "$R" "$*" "$N" >&2; exit 1; }

# ---------------------------------------------------------- ambiente -----
# WSL 2 ou Linux nativo: muda a dica de janelas gráficas e a GPU do emulador.
IS_WSL=false
grep -qi microsoft /proc/version 2>/dev/null && IS_WSL=true

# Distribuição e gerenciador de pacotes (Arch, Debian/Ubuntu, Fedora, openSUSE…).
DISTRO="linux"
if [ -r /etc/os-release ]; then DISTRO="$(. /etc/os-release && echo "${PRETTY_NAME:-${ID:-linux}}")"; fi
PKG_MANAGER=""
for pm in pacman apt-get dnf zypper; do
  if command -v "$pm" >/dev/null 2>&1; then PKG_MANAGER="$pm"; break; fi
done

# JDK 21: JAVA_HOME já apontando para um 21, mise, SDKMAN, /usr/lib/jvm ou o java do PATH.
java_major() { "$1/bin/java" -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -1; }
find_jdk21() {
  local c
  for c in "${JAVA_HOME_21:-}" "${JAVA_HOME:-}" \
           "$(command -v mise >/dev/null 2>&1 && mise where java@temurin-21 2>/dev/null || true)" \
           "$HOME"/.sdkman/candidates/java/21* /usr/lib/jvm/*21* /usr/lib/jvm/java-21*; do
    [ -n "$c" ] && [ -x "$c/bin/java" ] && [ "$(java_major "$c")" = "21" ] && { echo "$c"; return; }
  done
  if command -v java >/dev/null 2>&1; then
    c="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
    [ "$(java_major "$c")" = "21" ] && { echo "$c"; return; }
  fi
  return 0
}

# Android SDK: ANDROID_HOME, ANDROID_SDK_ROOT, sdk.dir do local.properties ou ~/Android/Sdk.
find_sdk() {
  local c from_props=""
  [ -r "$APP/local.properties" ] && from_props="$(sed -n 's/^sdk\.dir=//p' "$APP/local.properties" | head -1)"
  for c in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$from_props" "$HOME/Android/Sdk" "$HOME/Android/sdk"; do
    [ -n "$c" ] && [ -d "$c/platform-tools" ] && { echo "$c"; return; }
  done
  echo "${ANDROID_HOME:-$HOME/Android/Sdk}"
}

JDK="$(find_jdk21)"
SDK="$(find_sdk)"
ADB="$SDK/platform-tools/adb"
[ -n "$JDK" ] && export JAVA_HOME="$JDK" && export PATH="$JAVA_HOME/bin:$PATH"
export ANDROID_HOME="$SDK"

need_jdk() { [ -n "$JDK" ] || die "JDK 21 não encontrado. Rode 'scripts/rodar-app.sh check' para ver como instalar."; }
need_adb() { [ -x "$ADB" ] || die "Android SDK não encontrado em $SDK. Defina ANDROID_HOME ou rode 'scripts/rodar-app.sh check'."; }

# Comando que instala os pacotes de sistema que faltam, na sintaxe da distribuição.
install_cmd() {
  case "$PKG_MANAGER" in
    pacman)  echo "sudo pacman -S --needed $*" ;;
    apt-get) echo "sudo apt-get install -y $*" ;;
    dnf)     echo "sudo dnf install -y $*" ;;
    zypper)  echo "sudo zypper install -y $*" ;;
    *)       echo "instale pelo gerenciador de pacotes da sua distribuição: $*" ;;
  esac
}

# Biblioteca que o emulador pede → nome do pacote em cada família de distribuição.
lib_package() {
  local lib="$1"
  case "$PKG_MANAGER:$lib" in
    pacman:libpulse*) echo libpulse ;;     apt-get:libpulse*) echo libpulse0 ;;
    dnf:libpulse*) echo pulseaudio-libs ;; zypper:libpulse*) echo libpulse0 ;;
    pacman:libnss3*|pacman:libnssutil3*|pacman:libsmime3*) echo nss ;;
    apt-get:libnss3*|apt-get:libnssutil3*|apt-get:libsmime3*) echo libnss3 ;;
    dnf:libnss3*|dnf:libnssutil3*|dnf:libsmime3*) echo nss ;;
    zypper:libnss3*|zypper:libnssutil3*|zypper:libsmime3*) echo mozilla-nss ;;
    pacman:libnspr4*|pacman:libplc4*|pacman:libplds4*) echo nspr ;;
    apt-get:libnspr4*|apt-get:libplc4*|apt-get:libplds4*) echo libnspr4 ;;
    dnf:libnspr4*|dnf:libplc4*|dnf:libplds4*) echo nspr ;;
    zypper:libnspr4*|zypper:libplc4*|zypper:libplds4*) echo mozilla-nspr ;;
    pacman:libbsd*) echo libbsd ;;   apt-get:libbsd*) echo libbsd0 ;;   dnf:libbsd*) echo libbsd ;;   zypper:libbsd*) echo libbsd0 ;;
    pacman:libXi*) echo libxi ;;     apt-get:libXi*) echo libxi6 ;;     dnf:libXi*) echo libXi ;;     zypper:libXi*) echo libXi6 ;;
    pacman:libxkbfile*) echo libxkbfile ;; apt-get:libxkbfile*) echo libxkbfile1 ;;
    dnf:libxkbfile*) echo libxkbfile ;;    zypper:libxkbfile*) echo libxkbfile1 ;;
    pacman:libX11.*) echo libx11 ;;   apt-get:libX11.*) echo libx11-6 ;;   dnf:libX11.*) echo libX11 ;;   zypper:libX11.*) echo libX11-6 ;;
    pacman:libXext*) echo libxext ;;  apt-get:libXext*) echo libxext6 ;;   dnf:libXext*) echo libXext ;;  zypper:libXext*) echo libXext6 ;;
    pacman:libxcb.*) echo libxcb ;;   apt-get:libxcb.*) echo libxcb1 ;;    dnf:libxcb.*) echo libxcb ;;   zypper:libxcb.*) echo libxcb1 ;;
    pacman:libdbus*) echo dbus ;;     apt-get:libdbus*) echo "libdbus-1-3t64|libdbus-1-3" ;; dnf:libdbus*) echo dbus-libs ;; zypper:libdbus*) echo libdbus-1-3 ;;
    pacman:libdrm*) echo libdrm ;;    apt-get:libdrm*) echo libdrm2 ;;     dnf:libdrm*) echo libdrm ;;    zypper:libdrm*) echo libdrm2 ;;
    pacman:libexpat*) echo expat ;;   apt-get:libexpat*) echo libexpat1 ;; dnf:libexpat*) echo expat ;;   zypper:libexpat*) echo libexpat1 ;;
    pacman:libpng16*) echo libpng ;;  apt-get:libpng16*) echo "libpng16-16t64|libpng16-16" ;; dnf:libpng16*) echo libpng ;; zypper:libpng16*) echo libpng16-16 ;;
    *) echo "$lib" ;;
  esac
}

# Alguns pacotes mudaram de nome entre versões (Ubuntu 24.04 usa sufixo t64): escolhe o que existe.
resolve_package() {
  local alt
  if [ "$PKG_MANAGER" = "apt-get" ] && command -v apt-cache >/dev/null 2>&1; then
    for alt in ${1//|/ }; do
      apt-cache show "$alt" >/dev/null 2>&1 && { echo "$alt"; return; }
    done
  fi
  echo "${1%%|*}"
}

# Bibliotecas de sistema que o binário do emulador não encontra.
missing_emulator_libs() {
  local E="$SDK/emulator" bin="$SDK/emulator/qemu/linux-x86_64/qemu-system-x86_64"
  [ -x "$bin" ] || return 0
  LD_LIBRARY_PATH="$E/lib64:$E/lib64/qt/lib" ldd "$bin" 2>/dev/null | awk '/not found/ {print $1}' | sort -u || true
}

has_device() { [ -x "$ADB" ] && "$ADB" devices | tail -n +2 | grep -q "device$"; }

# ---------------------------------------------------------------- setup --
# Instala o que falta para compilar e rodar o app. Idempotente: pula o que já existe.
# Opções: --yes (não pergunta antes do sudo) e --no-emulator (só JDK + SDK, para usar o celular).
SDK_PACKAGES=("platform-tools" "platforms;android-36" "build-tools;36.0.0")
EMULATOR_PACKAGES=("emulator" "$AVD_IMAGE")

as_root() { if [ "$(id -u)" = "0" ]; then "$@"; else sudo "$@"; fi; }

# Pergunta antes de rodar algo com sudo (a menos que --yes). Sem terminal interativo, não roda.
confirm() {
  $ASSUME_YES && return 0
  [ -t 0 ] || { warn "sem terminal para confirmar; rode de novo com --yes ou execute o comando acima"; return 1; }
  local answer; read -r -p "  Executar agora? [s/N] " answer
  [[ "$answer" =~ ^[sSyY] ]]
}

run_pkg_install() {
  [ $# -gt 0 ] || return 0
  [ -n "$PKG_MANAGER" ] || { fail "gerenciador de pacotes não reconhecido; instale manualmente: $*"; return 1; }
  echo "    $(install_cmd "$*")"
  confirm || return 1
  case "$PKG_MANAGER" in
    pacman)  as_root pacman -S --needed --noconfirm "$@" ;;
    apt-get) as_root apt-get update -qq && DEBIAN_FRONTEND=noninteractive as_root apt-get install -y "$@" ;;
    dnf)     as_root dnf install -y "$@" ;;
    zypper)  as_root zypper --non-interactive install "$@" ;;
  esac
}

jdk_package() {
  case "$PKG_MANAGER" in
    pacman) echo jdk21-openjdk ;; apt-get) echo openjdk-21-jdk-headless ;;
    dnf | zypper) echo java-21-openjdk-devel ;;
  esac
}

setup_jdk() {
  if [ -n "$JDK" ]; then ok "JDK 21 já instalado ($JDK)"; return; fi
  echo "→ Instalando o JDK 21…"
  if command -v mise >/dev/null 2>&1; then
    mise install java@temurin-21
  elif [ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]; then
    # shellcheck disable=SC1091
    (set +u; . "$HOME/.sdkman/bin/sdkman-init.sh" && sdk install java 21-tem < /dev/null)
  else
    run_pkg_install "$(jdk_package)" || die "não foi possível instalar o JDK 21"
  fi
  JDK="$(find_jdk21)"
  [ -n "$JDK" ] || die "o JDK 21 foi instalado mas não foi encontrado; defina JAVA_HOME_21 e rode de novo"
  export JAVA_HOME="$JDK" PATH="$JDK/bin:$PATH"
  ok "JDK 21 em $JDK"
}

setup_tools() {
  local missing=()
  command -v unzip >/dev/null 2>&1 || missing+=(unzip)
  command -v curl >/dev/null 2>&1 || missing+=(curl)
  [ ${#missing[@]} -eq 0 ] && return
  echo "→ Faltam ferramentas para baixar o Android SDK: ${missing[*]}"
  run_pkg_install "${missing[@]}" || die "instale ${missing[*]} e rode de novo"
}

sdkmanager_bin() { echo "$SDK/cmdline-tools/latest/bin/sdkmanager"; }

setup_cmdline_tools() {
  [ -x "$(sdkmanager_bin)" ] && { ok "Android cmdline-tools já instalados ($SDK)"; return; }
  setup_tools
  echo "→ Baixando as Android cmdline-tools para $SDK…"
  local zip tmp
  zip="$(curl -fsSL https://dl.google.com/android/repository/repository2-3.xml \
    | grep -o 'commandlinetools-linux-[0-9]*_latest.zip' | sort -t- -k3 -n | tail -1)"
  [ -n "$zip" ] || die "não foi possível descobrir a versão das cmdline-tools (sem internet?)"
  tmp="$(mktemp -d)"
  curl -fL --progress-bar -o "$tmp/clt.zip" "https://dl.google.com/android/repository/$zip"
  unzip -q "$tmp/clt.zip" -d "$tmp"
  mkdir -p "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
  ok "cmdline-tools em $SDK/cmdline-tools/latest"
}

setup_sdk_packages() {
  local pkgs=("${SDK_PACKAGES[@]}") wanted=() p dir
  $WITH_EMULATOR && pkgs+=("${EMULATOR_PACKAGES[@]}")
  for p in "${pkgs[@]}"; do
    dir="$SDK/${p//;//}"
    [ -d "$dir" ] || wanted+=("$p")
  done
  if [ ${#wanted[@]} -eq 0 ]; then ok "pacotes do Android SDK já instalados"; return; fi
  echo "→ Aceitando as licenças do Android SDK e instalando: ${wanted[*]}"
  echo "  (as licenças: https://developer.android.com/studio/terms)"
  yes 2>/dev/null | "$(sdkmanager_bin)" --sdk_root="$SDK" --licenses >/dev/null 2>&1 || true
  # Mostra só o progresso dos downloads (o sdkmanager imprime avisos de "deprecated" e de métricas).
  "$(sdkmanager_bin)" --sdk_root="$SDK" "${wanted[@]}" 2>&1 \
    | grep -E '^https://dl\.google\.com|^Unzipping|[Ee]rror|[Ff]ailed' || true
  ok "pacotes do Android SDK instalados"
}

setup_avd() {
  $WITH_EMULATOR || return 0
  if "$SDK/emulator/emulator" -list-avds 2>/dev/null | grep -qx "$AVD"; then ok "AVD $AVD já existe"; return; fi
  echo "→ Criando o AVD $AVD (Pixel 7, Android 36)…"
  echo no | "$SDK/cmdline-tools/latest/bin/avdmanager" create avd -n "$AVD" -k "$AVD_IMAGE" -d pixel_7 --force >/dev/null
  ok "AVD $AVD criado"
}

setup_emulator_libs() {
  $WITH_EMULATOR || return 0
  local libs pkgs=() l
  libs="$(missing_emulator_libs)"
  if [ -z "$libs" ]; then ok "bibliotecas do emulador já instaladas"; return; fi
  for l in $libs; do pkgs+=("$(resolve_package "$(lib_package "$l")")"); done
  mapfile -t pkgs < <(printf '%s\n' "${pkgs[@]}" | sort -u)
  echo "→ O emulador precisa de bibliotecas do sistema ($(echo $libs | wc -w) arquivos):"
  if run_pkg_install "${pkgs[@]}"; then ok "bibliotecas do emulador instaladas"
  else warn "bibliotecas não instaladas: o emulador não vai abrir até instalar (o celular continua funcionando)"; fi
}

setup_kvm() {
  $WITH_EMULATOR || return 0
  if [ -r /dev/kvm ] && [ -w /dev/kvm ]; then ok "/dev/kvm acessível"; return; fi
  if [ -e /dev/kvm ] && getent group kvm >/dev/null; then
    echo "→ Seu usuário não tem acesso ao /dev/kvm (aceleração do emulador):"
    echo "    sudo usermod -aG kvm $USER"
    if confirm; then as_root usermod -aG kvm "$USER" && warn "entre de novo na sessão (ou 'newgrp kvm') para valer"; fi
  else
    warn "/dev/kvm ausente: o emulador fica lento ou não abre; use o celular (make phone) ou ative a virtualização"
  fi
}

setup_local_properties() {
  if grep -q "^sdk.dir=" "$APP/local.properties" 2>/dev/null; then ok "local.properties já existe"; return; fi
  echo "sdk.dir=$SDK" >> "$APP/local.properties"
  ok "local.properties criado (sdk.dir=$SDK)"
}

setup_env_key() {
  if grep -q "^ELEICOES_API_KEY=." "$REPO/.env" 2>/dev/null || [ -n "${ELEICOES_API_KEY:-}" ]; then ok "chave da API configurada"; return; fi
  warn "falta a chave da API (pessoal, não é instalada pelo script):"
  echo "    1. crie uma grátis em https://eleicoes.dev/dashboard"
  echo "    2. echo 'ELEICOES_API_KEY=<sua chave>' > $REPO/.env   (nunca commite)"
}

setup() {
  ASSUME_YES=false; WITH_EMULATOR=true
  local arg
  for arg in "$@"; do
    case "$arg" in
      -y | --yes) ASSUME_YES=true ;;
      --no-emulator) WITH_EMULATOR=false ;;
      *) die "opção desconhecida para setup: $arg (use --yes e/ou --no-emulator)" ;;
    esac
  done
  echo "Setup em $DISTRO$($IS_WSL && echo ' (WSL 2)')${PKG_MANAGER:+ · pacotes: $PKG_MANAGER}$($WITH_EMULATOR || echo ' · sem emulador')"
  $WITH_EMULATOR && echo "Primeira vez: ~2 GB de download (SDK + imagem do Android 36). Pode rodar de novo: pula o que já existe."
  setup_jdk
  setup_cmdline_tools
  export ANDROID_HOME="$SDK"
  setup_sdk_packages
  ADB="$SDK/platform-tools/adb"
  setup_avd
  setup_emulator_libs
  setup_kvm
  setup_local_properties
  setup_env_key
  echo
  check
}

# ------------------------------------------------------------- comandos --
check() {
  echo "Sistema: $DISTRO$($IS_WSL && echo ' (WSL 2)')${PKG_MANAGER:+ · pacotes: $PKG_MANAGER}"
  echo "Ambiente:"
  if [ -n "$JDK" ]; then ok "JDK 21 ($JDK)"; else
    fail "JDK 21 não encontrado. Opções:"
    echo "      mise install java@temurin-21   (ou SDKMAN: sdk install java 21-tem)"
    echo "      $(install_cmd "$(case $PKG_MANAGER in pacman) echo jdk21-openjdk;; apt-get) echo openjdk-21-jdk;; dnf) echo java-21-openjdk-devel;; zypper) echo java-21-openjdk-devel;; *) echo openjdk-21;; esac)")"
  fi
  if [ -x "$ADB" ]; then ok "Android SDK em $SDK"; else
    fail "Android SDK não encontrado (defina ANDROID_HOME). Instale as cmdline-tools em ~/Android/Sdk e rode:"
    echo "      sdkmanager \"platform-tools\" \"platforms;android-36\" \"build-tools;36.0.0\" \"emulator\" \"$AVD_IMAGE\""
  fi
  if grep -q "^sdk.dir=" "$APP/local.properties" 2>/dev/null; then ok "local.properties"; else
    fail "local.properties: echo \"sdk.dir=$SDK\" > $APP/local.properties"
  fi
  if grep -q "^ELEICOES_API_KEY=." "$REPO/.env" 2>/dev/null || [ -n "${ELEICOES_API_KEY:-}" ]; then ok "chave da API (.env ou ELEICOES_API_KEY)"; else
    fail "chave da API: crie $REPO/.env com ELEICOES_API_KEY=<sua chave> (https://eleicoes.dev/dashboard)"
  fi
  if [ -r /dev/kvm ] && [ -w /dev/kvm ]; then ok "/dev/kvm (aceleração do emulador)"; else
    if [ -e /dev/kvm ]; then fail "/dev/kvm sem permissão: sudo usermod -aG kvm \$USER (e entre de novo na sessão)"
    elif $IS_WSL; then fail "/dev/kvm ausente: ative a virtualização aninhada no WSL ou use o celular (phone)"
    else fail "/dev/kvm ausente: ative a virtualização (VT-x/AMD-V) na BIOS ou use o celular (phone)"; fi
  fi
  if [ -n "${WAYLAND_DISPLAY:-}${DISPLAY:-}" ]; then ok "janelas gráficas ($($IS_WSL && echo WSLg || echo "${XDG_SESSION_TYPE:-X11/Wayland}"))"; else
    if $IS_WSL; then fail "WSLg ausente: rode 'wsl --update' no PowerShell"; else fail "sem sessão gráfica (DISPLAY/WAYLAND_DISPLAY vazios)"; fi
  fi
  if [ ! -x "$SDK/emulator/emulator" ]; then warn "emulador não instalado (só é preciso sem celular): sdkmanager \"emulator\" \"$AVD_IMAGE\""
  else
    local libs pkgs
    libs="$(missing_emulator_libs | tr '\n' ' ')"
    if [ -z "${libs// /}" ]; then ok "bibliotecas do emulador"; else
      pkgs="$(for l in $libs; do resolve_package "$(lib_package "$l")"; done | sort -u | tr '\n' ' ')"
      fail "faltam bibliotecas para o emulador: $libs"
      echo "      $(install_cmd "$pkgs")"
    fi
    if "$SDK/emulator/emulator" -list-avds 2>/dev/null | grep -qx "$AVD"; then ok "AVD $AVD"; else
      warn "AVD $AVD não existe: avdmanager create avd -n $AVD -k \"$AVD_IMAGE\" -d pixel_7"
    fi
  fi
  echo "Aparelhos conectados:"
  if [ -x "$ADB" ]; then "$ADB" devices | tail -n +2 | sed '/^$/d;s/^/  /'; fi
}

emulator() {
  need_adb
  [ -x "$SDK/emulator/emulator" ] || die "Emulador não instalado. Rode 'scripts/rodar-app.sh check'."
  if has_device; then ok "já há aparelho conectado ($("$ADB" devices | awk 'NR>1 && $2=="device" {print $1; exit}'))"; return; fi
  local gpu="auto"; $IS_WSL && gpu="swiftshader_indirect"
  echo "Ligando o emulador $AVD$($IS_WSL && echo ' (a janela abre no Windows pelo WSLg)')…"
  nohup "$SDK/emulator/emulator" -avd "$AVD" -gpu "$gpu" -no-audio -no-boot-anim \
    -no-snapshot-save -memory 3072 > "$LOG" 2>&1 &
  "$ADB" wait-for-device
  until [ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do sleep 2; done
  ok "emulador pronto ($("$ADB" devices | awk 'NR>1 && $2=="device" {print $1; exit}'))"
}

stop() {
  need_adb
  if "$ADB" devices | grep -q "^emulator-"; then "$ADB" emu kill >/dev/null 2>&1 || true; ok "emulador desligado"
  else warn "nenhum emulador ligado"; fi
}

phone() {
  need_adb
  [ $# -eq 3 ] || die "uso: scripts/rodar-app.sh phone IP:PORTA_PAREAMENTO CODIGO IP:PORTA_CONEXAO"
  "$ADB" pair "$1" "$2"
  "$ADB" connect "$3"
  "$ADB" devices
}

install() {
  need_jdk; need_adb
  # Sem aparelho conectado o Gradle falha com "No connected devices!": liga o emulador antes.
  if ! has_device; then echo "Nenhum aparelho conectado."; emulator; fi
  (cd "$APP" && ./gradlew :androidApp:installDebug)
  "$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
  ok "app instalado e aberto"
}

open_app() {
  need_adb
  has_device || die "nenhum aparelho conectado: rode 'scripts/rodar-app.sh emulator'"
  "$ADB" shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1 >/dev/null
  ok "app aberto"
}

shot() {
  need_adb
  local dir="$REPO/scripts/prints"; mkdir -p "$dir"
  "$ADB" exec-out screencap -p > "$dir/${1:-print}.png"
  ok "print salvo em $dir/${1:-print}.png"
}

rotate() { need_adb; "$ADB" emu rotate >/dev/null; ok "tela girada 90°"; }

gradle() { need_jdk; (cd "$APP" && ./gradlew "$@"); }
build()       { gradle :androidApp:assembleDebug "$@"; ok "APK em $APP/androidApp/build/outputs/apk/debug/"; }
test_all()    { gradle :shared:testAndroidHostTest :androidApp:testDebugUnitTest "$@"; }
test_shared() { gradle :shared:testAndroidHostTest "$@"; }
test_app()    { gradle :androidApp:testDebugUnitTest "$@"; }
ci()          { gradle :androidApp:assembleDebug :shared:testAndroidHostTest :androidApp:testDebugUnitTest "$@"; }
clean()       { gradle clean "$@"; }

# Abre os relatórios HTML dos testes no navegador (Windows pelo WSL, ou xdg-open no Linux).
reports() {
  local f opened=false
  for f in "$APP/shared/build/reports/tests/testAndroidHostTest/index.html" \
           "$APP/androidApp/build/reports/tests/testDebugUnitTest/index.html"; do
    [ -f "$f" ] || continue
    if $IS_WSL && command -v explorer.exe >/dev/null 2>&1; then explorer.exe "$(wslpath -w "$f")" || true
    elif command -v xdg-open >/dev/null 2>&1; then xdg-open "$f" >/dev/null 2>&1 &
    else echo "  $f"; fi
    opened=true
  done
  $opened || die "nenhum relatório ainda: rode 'scripts/rodar-app.sh test' antes"
}

logs() { [ -f "$LOG" ] && tail -n "${1:-40}" "$LOG" || die "sem log em $LOG"; }

help() {
  cat <<AJUDA
Uso: scripts/rodar-app.sh <comando> [argumentos]      (ou: make <alvo>)

Roda, testa e compila o app eleicoes-kmp (projeto final) em qualquer Linux, nativo ou WSL 2.
Detecta sozinho: distribuição e gerenciador de pacotes, JDK 21 (JAVA_HOME, mise, SDKMAN,
/usr/lib/jvm ou PATH) e Android SDK (ANDROID_HOME, ANDROID_SDK_ROOT, local.properties ou
~/Android/Sdk).

Instalação
  setup [--yes] [--no-emulator]
                        Instala o que faltar: JDK 21 (mise, SDKMAN ou pacote da distro),
                        Android SDK (cmdline-tools, platform-tools, platform 36, build-tools),
                        emulador + imagem + AVD "$AVD", bibliotecas do sistema (pede
                        confirmação antes do sudo), local.properties. Aceita as licenças
                        do Android SDK. Não instala o que já existe.
                        --yes: não pergunta antes do sudo · --no-emulator: só para celular.

Aparelho
  check                 Confere o ambiente e diz como instalar o que faltar, no comando
                        da sua distribuição (pacman, apt, dnf ou zypper).
  emulator              Liga o emulador "$AVD" e espera o Android iniciar (não faz nada
                        se já houver aparelho conectado).
  stop                  Desliga o emulador.
  phone P_PAR COD P_CON Conecta um celular por depuração sem fio:
                        P_PAR = IP:porta de pareamento, COD = código de 6 dígitos,
                        P_CON = IP:porta de conexão (Opções do desenvolvedor →
                        Depuração sem fio).
  rotate                Gira a tela do emulador 90°.
  logs [N]              Últimas N linhas do log do emulador (padrão 40).

App
  install               Compila, instala e abre o app (liga o emulador se preciso).
  open                  Abre o app já instalado.
  shot [NOME]           Salva um print da tela em scripts/prints/NOME.png.

Build e testes (argumentos extras vão para o Gradle, ex.: --tests "*Partidos*")
  build                 Compila o APK de debug.
  test                  Todos os testes (shared + androidApp).
  test-shared           Testes do domínio e dos dados (shared).
  test-app              Testes dos ViewModels (androidApp).
  ci                    O mesmo comando do CI: build + todos os testes.
  reports               Abre os relatórios HTML dos testes no navegador.
  clean                 Limpa os builds.

  help                  Mostra esta ajuda (também sem comando, -h ou --help).

Exemplos
  scripts/rodar-app.sh setup
  scripts/rodar-app.sh check
  scripts/rodar-app.sh install
  scripts/rodar-app.sh test-shared --tests "*PartidosTest*"
  scripts/rodar-app.sh phone 192.168.0.10:37123 123456 192.168.0.10:41234

Variáveis opcionais: AVD (padrão $AVD), AVD_IMAGE, ANDROID_HOME, JAVA_HOME_21.
Girar a tela também funciona com Ctrl+← / Ctrl+→ na janela do emulador.
AJUDA
}

cmd="${1:-help}"; shift || true
case "$cmd" in
  setup | check | emulator | stop | phone | rotate | logs | install | shot | build | ci | clean | reports | help) "$cmd" "$@" ;;
  test) test_all "$@" ;;
  open) open_app "$@" ;;
  test-shared) test_shared "$@" ;;
  test-app) test_app "$@" ;;
  -h | --help) help ;;
  *) echo "Comando desconhecido: $cmd" >&2; echo >&2; help >&2; exit 1 ;;
esac
