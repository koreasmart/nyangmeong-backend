#!/usr/bin/env bash
# 냥멍케어 백엔드 개발 환경 설치. 맥은 터미널에서 `bash setup.sh`, Windows는 cmd·PowerShell에서 `setup.bat`(또는 Git Bash에서 `bash setup.sh`)로 실행한다.
# 이미 맞는 버전이 있으면 건너뛴다. 여러 번 실행해도 된다.
# 프론트 저장소의 setup.sh와 공통 부분(맨 위 함수와 "공통" 절)이 같다. 고칠 때 둘 다 고친다.
set -euo pipefail
cd "$(dirname "$0")"

TODO=0
NEED_RESTART=0
ok()   { printf '  [완료] %s\n' "$1"; }
skip() { printf '  [있음] %s\n' "$1"; }
todo() { printf '  [직접] %s\n' "$1"; TODO=$((TODO + 1)); }
die()  { printf '\n[중단] %s\n' "$1" >&2; exit 1; }
step() { printf '\n== %s\n' "$1"; }

case "$(uname -s)" in
  Darwin) OS=mac ;;
  MINGW* | MSYS*) OS=win ;;
  *) die "지원하지 않는 환경입니다: $(uname -s). 맥은 터미널에서 bash setup.sh, Windows는 setup.bat로 실행해 주세요." ;;
esac

# 패키지 설치. 맥은 Homebrew, Windows는 winget
# pkg <brew 이름> <formula|cask> <winget ID> [winget 버전]
pkg() {
  if [ "$OS" = mac ]; then
    command -v brew >/dev/null || die "Homebrew가 없습니다. https://brew.sh 의 설치 명령을 먼저 실행해 주세요."
    if [ "$2" = cask ]; then brew install --cask "$1"; else brew install "$1"; fi
  else
    command -v winget.exe >/dev/null || die "winget이 없습니다. Microsoft Store에서 '앱 설치 관리자'를 설치해 주세요."
    if [ -n "${4:-}" ]; then
      winget.exe install --id "$3" -e --version "$4" --accept-package-agreements --accept-source-agreements
    else
      winget.exe install --id "$3" -e --accept-package-agreements --accept-source-agreements
    fi
    # winget이 바꾼 PATH는 새 창에서만 적용된다
    NEED_RESTART=1
  fi
}

# 셸 설정 파일에 한 줄을 넣는다. 이미 있으면 그대로 둔다
persist_line() {
  mkdir -p "$(dirname "$1")"
  touch "$1"
  grep -qxF "$2" "$1" || printf '\n%s\n' "$2" >> "$1"
}

# 새 창에서도 이 폴더의 명령이 잡히도록 PATH에 영구로 넣는다. 맥은 ~/.zshrc, Windows는 사용자 PATH
persist_path() {
  if [ "$OS" = mac ]; then
    persist_line "$HOME/.zshrc" "export PATH=\"$1:\$PATH\""
    return
  fi
  # %USERPROFILE% 같은 항목이 풀리지 않도록 레지스트리 값을 ExpandString으로 그대로 다시 쓴다.
  # 이미 있는지는 양쪽 값을 풀고 끝의 역슬래시와 대소문자를 무시해 비교한다(%USERPROFILE% 형태로 들어 있어도 같은 폴더로 본다)
  # 마지막 줄은 열려 있는 탐색기에 환경변수 변경을 알리려고 빈 변수를 지우는 것이다
  WIN_DIR="$(cygpath -w "$1")" powershell.exe -NoProfile -Command '
    $key = [Microsoft.Win32.Registry]::CurrentUser.OpenSubKey("Environment", $true)
    $parts = @(([string]$key.GetValue("Path", "", "DoNotExpandEnvironmentNames")) -split ";" | Where-Object { $_ })
    $norm = { param($p) [Environment]::ExpandEnvironmentVariables($p).TrimEnd("\").ToLowerInvariant() }
    $want = & $norm $env:WIN_DIR
    if (-not ($parts | Where-Object { (& $norm $_) -eq $want })) {
      $key.SetValue("Path", (($parts + $env:WIN_DIR) -join ";"), [Microsoft.Win32.RegistryValueKind]::ExpandString)
      [Environment]::SetEnvironmentVariable("NYANGMEONG_SETUP", $null, "User")
    }'
}

# VS Code 명령줄 도구 위치. 맥은 PATH에 없으면 앱 안의 것을 쓴다
vscode_cli() {
  if command -v code >/dev/null; then command -v code; return; fi
  if [ "$OS" = mac ]; then
    local app
    app=$(mdfind "kMDItemCFBundleIdentifier == 'com.microsoft.VSCode'" 2>/dev/null | head -1)
    [ -n "$app" ] && [ -x "$app/Contents/Resources/app/bin/code" ] && echo "$app/Contents/Resources/app/bin/code"
  fi
  return 0
}

# .vscode/extensions.json의 확장을 설치한다. VS Code가 없으면 직접 할 일로 남긴다
vscode_extensions() {
  local code installed ext
  code=$(vscode_cli)
  if [ -z "$code" ]; then
    todo "VS Code를 설치한 뒤 다시 실행해 주세요(확장 설치를 위해)."
    return 0
  fi
  installed=$("$code" --list-extensions | tr '[:upper:]' '[:lower:]')
  for ext in $(grep -o '"[^"]*\.[^"]*"' .vscode/extensions.json | tr -d '"'); do
    if printf '%s\n' "$installed" | grep -qx "$(printf '%s' "$ext" | tr '[:upper:]' '[:lower:]')"; then
      skip "VS Code 확장 $ext"
    else
      "$code" --install-extension "$ext" >/dev/null && ok "VS Code 확장 $ext"
    fi
  done
}

# MCP 서버를 감지한 AI 도구에 사용자 범위로 등록한다. 원격 서버의 인증은 OAuth라 브라우저에서 한다
# mcp_add <이름> <URL>              원격(HTTP) 서버
# mcp_add <이름> -- <명령> [인자...]  이 PC에서 띄우는(stdio) 서버
# claude.ai에서 연결한 커넥터(claude.ai <이름>) 중 이름(대소문자 무시)이나 주소가 같은 줄을 출력한다. 커넥터는 claude mcp get 으로 찾을 수 없다
# claude_connector <이름> [URL]. $(...) 안에서 부르므로 목록은 그 전에 claude_ai_list_load 로 읽어 둔다
claude_connector() {
  claude_ai_list_load
  printf '%s\n' "$claude_ai_list" | grep -i -m1 "^claude\.ai $1:" && return 0
  [ -n "${2:-}" ] && printf '%s\n' "$claude_ai_list" | grep -F -m1 ": $2 " && return 0
  return 0
}
# claude mcp list 는 서버마다 연결을 확인해 느리므로 한 번만 읽는다
claude_ai_list_load() {
  [ -n "${claude_ai_list+x}" ] || claude_ai_list=$(claude mcp list 2>/dev/null | grep '^claude\.ai ' || true)
}

mcp_add() {
  local name=$1 connector=""
  shift
  if use_claude; then
    # 같은 커넥터가 이미 연결돼 있으면 다시 등록하지 않는다
    claude_ai_list_load
    if [ "$1" = -- ]; then connector=$(claude_connector "$name"); else connector=$(claude_connector "$name" "$1"); fi
    if [ -n "$connector" ]; then
      skip "Claude Code MCP $name (${connector%%:*} 커넥터)"
    elif claude mcp get "$name" >/dev/null 2>&1; then
      skip "Claude Code MCP $name"
    elif [ "$1" = -- ]; then
      claude mcp add -s user "$name" "$@" >/dev/null && ok "Claude Code MCP $name"
    else
      claude mcp add -s user -t http "$name" "$1" >/dev/null && ok "Claude Code MCP $name"
    fi
    if [ -z "$connector" ] && claude mcp get "$name" 2>/dev/null | grep -q 'Needs authentication'; then
      todo "claude 를 실행해 /mcp 에서 $name 를 고르고 Authenticate 해 주세요"
    fi
  fi
  if use_codex; then
    if codex mcp get "$name" >/dev/null 2>&1; then
      skip "Codex MCP $name"
    elif [ "$1" = -- ]; then
      codex mcp add "$name" "$@" >/dev/null && ok "Codex MCP $name"
    # 원격 서버는 등록하면 바로 브라우저 로그인이 시작된다
    elif codex mcp add "$name" --url "$1"; then
      ok "Codex MCP $name"
    else
      todo "codex mcp login $name 로 로그인해 주세요"
    fi
  fi
}

# 이니셜 a가 b와 한 글자만 다르거나 이웃한 두 글자만 바뀌었으면 참
near() {
  local a=$1 b=$2 i diff=""
  [ "${#a}" = "${#b}" ] || return 1
  for ((i = 0; i < ${#a}; i++)); do
    [ "${a:i:1}" = "${b:i:1}" ] || diff="$diff $i"
  done
  set -- $diff
  [ $# = 1 ] && return 0
  [ $# = 2 ] && [ $(($2 - $1)) = 1 ] && [ "${a:$1:1}" = "${b:$2:1}" ] && [ "${a:$2:1}" = "${b:$1:1}" ]
}

# 입력을 대문자로 바꿔 INITIALS 중 하나로 맞춘다. 같으면 그것, 비슷한 것이 하나면 확인 후 그것, 아니면 다시 묻는다
ask_initials() {
  local input c found answer
  while :; do
    read -rp "  개인 브랜치로 쓸 이니셜을 입력하세요($INITIALS 중 하나): " input
    input=$(printf '%s' "$input" | tr -d '[:space:]' | tr '[:lower:]' '[:upper:]')
    [ -n "$input" ] || continue
    found=""
    for c in $INITIALS; do
      if [ "$c" = "$input" ]; then initials=$c; return 0; fi
      if near "$input" "$c"; then found="$found $c"; fi
    done
    set -- $found
    case $# in
      0) printf '  %s 은(는) 목록에 없습니다.\n' "$input" ;;
      1)
        read -rp "  -> $1 으로 진행할까요? [Y/n] " answer
        case "$answer" in [nN]*) ;; *) initials=$1; return 0 ;; esac
        ;;
      *) printf '  %s 와 비슷한 이니셜이 여럿입니다:%s. 정확히 입력해 주세요.\n' "$input" "$found" ;;
    esac
  done
}

step "공통"

command -v git >/dev/null || die "Git이 없습니다. Windows는 Git for Windows를 설치한 뒤 다시 실행해 주세요."
skip "Git $(git --version | awk '{print $3}')"

# AI 도구: 설치된 Claude Code·Codex를 감지해 그 도구에 MCP를 등록한다. 둘 다 없으면 Claude Code를 설치한다.
# Codex만 쓰려면 Codex를 먼저 설치하고 실행한다. 다시 실행하면 새로 설치한 도구도 감지한다
has_claude=n
has_codex=n
if command -v claude >/dev/null; then
  has_claude=y
  skip "Claude Code $(claude --version 2>/dev/null | awk '{print $1}')"
fi
if command -v codex >/dev/null; then
  has_codex=y
  skip "Codex $(codex --version 2>/dev/null | awk '{print $NF}')"
fi
# Claude Code: 공식 설치 스크립트가 ~/.local/bin에 설치한다. 로그인은 처음 실행할 때 직접 한다
if [ "$has_claude$has_codex" = nn ]; then
  if [ "$OS" = mac ]; then
    curl -fsSL https://claude.ai/install.sh | bash
  else
    powershell.exe -NoProfile -ExecutionPolicy Bypass -Command 'irm https://claude.ai/install.ps1 | iex'
  fi
  export PATH="$HOME/.local/bin:$PATH"
  command -v claude >/dev/null || die "Claude Code 설치에 실패했습니다. 위 출력을 확인해 주세요."
  persist_path "$HOME/.local/bin"
  has_claude=y
  ok "Claude Code $(claude --version 2>/dev/null | awk '{print $1}')"
  todo "저장소 폴더에서 claude 를 실행해 로그인해 주세요"
fi
use_claude() { [ "$has_claude" = y ]; }
use_codex() { [ "$has_codex" = y ]; }

# 전역 git 설정(회사 계정 등)은 건드리지 않고 이 저장소에만 둔다. 전역 값이 있으면 그대로 쓸지 묻는다
for key in user.name user.email; do
  if [ -n "$(git config --local "$key" || true)" ]; then
    skip "git $key = $(git config --local "$key")"
  else
    value=$(git config --global "$key" || true)
    if [ -n "$value" ]; then
      read -rp "  이 저장소에도 전역 git $key($value) 값을 사용하시겠습니까? [Y/n] " answer
      case "$answer" in [nN]*) value="" ;; esac
    fi
    while [ -z "$value" ]; do read -rp "  이 저장소에 사용할 git $key 값을 입력하세요: " value; done
    git config --local "$key" "$value"
    ok "git $key = $value"
  fi
done

# 커밋 훅: 커밋 메시지에서 AI 도구가 붙인 Co-Authored-By 줄을 지운다(.githooks/commit-msg, docs/convention/git.md)
if [ "$(git config --local core.hooksPath || true)" = .githooks ]; then
  skip "git 커밋 훅 (.githooks)"
else
  git config --local core.hooksPath .githooks
  ok "git 커밋 훅 (.githooks)"
fi

# 개인 브랜치 이름 = 이니셜. 핸드오프 파일 이름과 커밋 접두사가 여기서 나온다.
# 쓸 수 있는 이니셜은 마일스톤 담당 표(docs/convention/milestone.md "담당" 절)의 첫 열이다
INITIALS=$(awk '/^## /{f = ($0 == "## 담당")} f && /^\| [A-Z]+ \|/{print $2}' docs/convention/milestone.md | tr '\n' ' ' | sed 's/ $//')
[ -n "$INITIALS" ] || die "docs/convention/milestone.md 의 담당 표에서 이니셜을 읽지 못했습니다."
branch=$(git branch --show-current)
branch_ok=y
case "$branch" in
  "" | main | master | develop)
    ask_initials
    if git show-ref --verify --quiet "refs/heads/$initials"; then
      git checkout "$initials"
    elif git ls-remote --exit-code --heads origin "$initials" >/dev/null 2>&1; then
      git fetch origin "$initials" && git checkout -b "$initials" --track "origin/$initials"
    else
      git checkout -b "$initials"
    fi
    branch=$initials
    ok "브랜치 $branch"
    ;;
  *)
    if printf ' %s ' "$INITIALS" | grep -qF " $branch "; then
      skip "브랜치 $branch"
    else
      # 이니셜이 아닌 브랜치는 올리지 않고 이름을 바꾸라고 안내한다. 대소문자만 다르면 -M 이어야 맥·Windows에서 바뀐다
      branch_ok=n
      upper=$(printf '%s' "$branch" | tr '[:lower:]' '[:upper:]')
      if printf ' %s ' "$INITIALS" | grep -qF " $upper "; then
        todo "브랜치 $branch 는 대문자 이니셜이 아닙니다. git branch -M $upper 로 바꾼 뒤 다시 실행해 주세요"
      else
        todo "브랜치 $branch 는 이니셜($INITIALS)이 아닙니다. git checkout develop 한 뒤 다시 실행해 이니셜 브랜치를 만들어 주세요"
      fi
    fi
    ;;
esac

# 원격에 없는 개인 브랜치는 공개 저장소이므로 물어보고 올린다
if [ "$branch_ok" = y ] && ! git ls-remote --exit-code --heads origin "$branch" >/dev/null 2>&1; then
  read -rp "  origin에 $branch 브랜치가 없습니다. 지금 올리시겠습니까? [Y/n] " answer
  case "$answer" in
    [nN]*) todo "개인 브랜치를 올려 주세요: git push -u origin $branch" ;;
    *)
      if git push -q -u origin "$branch"; then ok "브랜치 $branch 푸시"
      else todo "푸시하지 못했습니다. 저장소 권한을 확인한 뒤 git push -u origin $branch 를 실행해 주세요"; fi
      ;;
  esac
fi

step "백엔드"

# IDE: VS Code·STS 중 쓰는 것만 세팅한다. 고른 값은 이 저장소의 git 설정(nyangmeong.ide)에 기억한다. 다시 고르려면 --ide 를 붙인다
ide=$(git config --local nyangmeong.ide || true)
for arg in "$@"; do
  case "$arg" in --ide) ide="" ;; esac
done
if [ -z "$ide" ]; then
  echo "  사용할 IDE를 고르세요: 1) VS Code  2) STS  3) 둘 다"
  while :; do
    read -rp "  번호 [1]: " answer
    case "${answer:-1}" in
      1) ide=vscode; break ;;
      2) ide=sts; break ;;
      3) ide=both; break ;;
    esac
  done
  git config --local nyangmeong.ide "$ide"
  ok "IDE $ide"
else
  skip "IDE $ide (다시 고르려면 --ide)"
fi

# JDK: build.gradle의 toolchain 버전. 배포판(Temurin, Oracle, Microsoft 등)은 가리지 않는다.
# 버전은 폴더 이름이 아니라 JDK의 release 파일로 확인한다
want_jdk=$(grep -o 'JavaLanguageVersion.of([0-9]*)' build.gradle | grep -o '[0-9][0-9]*')
jdk_major() { sed -n 's/^JAVA_VERSION="\([0-9]*\).*/\1/p' "$1/release" 2>/dev/null; }
have_jdk=""
jdk_auto=n  # VS Code Java 확장이 자동으로 찾는 위치(표준 설치 위치, JAVA_HOME)에 있는가
if [ "$OS" = mac ]; then
  d=$(/usr/libexec/java_home -v "$want_jdk" 2>/dev/null || true)
  [ -n "$d" ] && [ "$(jdk_major "$d")" = "$want_jdk" ] && { have_jdk=$d; jdk_auto=y; }
else
  for d in "/c/Program Files/"{"Eclipse Adoptium",Java,Microsoft,Zulu,"Amazon Corretto",BellSoft}/*; do
    [ "$(jdk_major "$d")" = "$want_jdk" ] && { have_jdk=$d; jdk_auto=y; }
  done
fi
if [ -z "$have_jdk" ] && [ -n "${JAVA_HOME:-}" ]; then
  d=$JAVA_HOME
  [ "$OS" = win ] && d=$(cygpath -u "$d")
  [ "$(jdk_major "$d")" = "$want_jdk" ] && { have_jdk=$d; jdk_auto=y; }
fi
# IntelliJ가 내려받은 JDK(~/.jdks)는 VS Code가 자동으로 찾지 못할 수 있다
if [ -z "$have_jdk" ]; then
  for d in "$HOME"/.jdks/*; do
    [ "$(jdk_major "$d")" = "$want_jdk" ] && have_jdk=$d
  done
fi
if [ -n "$have_jdk" ]; then
  skip "JDK $want_jdk ($have_jdk)"
else
  pkg "temurin@$want_jdk" cask "EclipseAdoptium.Temurin.$want_jdk.JDK"
  ok "JDK $want_jdk"
fi

# VS Code: 1차는 Java 확장의 자동 감지에 맡긴다. 자동 감지 위치가 아닌 JDK만 사용자 설정의 java.configuration.runtimes에 넣는다.
# 경로가 PC마다 다르므로 저장소의 .vscode/settings.json이 아니라 사용자 설정에 쓴다
[ "$ide" = sts ] || vscode_extensions
if [ "$ide" = sts ]; then
  :  # VS Code를 쓰지 않는다
elif [ -n "$have_jdk" ] && [ "$jdk_auto" = y ]; then
  skip "VS Code JDK $want_jdk (자동 감지)"
elif [ -n "$have_jdk" ]; then
  if [ "$OS" = mac ]; then
    user_settings="$HOME/Library/Application Support/Code/User/settings.json"
    jdk_path=$have_jdk
  else
    user_settings="$(cygpath -u "$APPDATA")/Code/User/settings.json"
    jdk_path=$(cygpath -m "$have_jdk")
  fi
  if [ -f "$user_settings" ] && grep -q '"java.configuration.runtimes"' "$user_settings"; then
    # 사용자가 직접 관리하는 값이므로 고치지 않는다
    if grep -qF "\"$jdk_path\"" "$user_settings"; then
      skip "VS Code JDK $want_jdk (java.configuration.runtimes)"
    else
      todo "VS Code 사용자 설정의 java.configuration.runtimes 에 { \"name\": \"JavaSE-$want_jdk\", \"path\": \"$jdk_path\" } 를 추가해 주세요"
    fi
  else
    entry="  \"java.configuration.runtimes\": [{ \"name\": \"JavaSE-$want_jdk\", \"path\": \"$jdk_path\" }]"
    mkdir -p "$(dirname "$user_settings")"
    if [ -s "$user_settings" ]; then
      # 첫 번째 { 바로 뒤에 한 줄을 넣는다. VS Code 설정은 끝에 붙은 쉼표를 허용한다
      awk -v e="$entry," '!done && /\{/ { sub(/\{/, "{\n" e); done = 1 } { print }' "$user_settings" > "$user_settings.tmp" \
        && mv "$user_settings.tmp" "$user_settings"
    else
      printf '{\n%s\n}\n' "$entry" > "$user_settings"
    fi
    ok "VS Code JDK $want_jdk (자동 감지 위치가 아니라 java.configuration.runtimes 에 등록)"
  fi
fi

# STS: 없으면 설치한다. 맥은 brew, Windows는 winget의 STS가 버전이 늦어서 공식 zip을 받아 사용자 폴더에 푼다.
# Windows 버전은 맥 brew(springtoolsforeclipse)와 맞춰 둔다
STS_VERSION=5.4.0
STS_ECLIPSE=4.41.0
find_sts() {
  sts_dir=""
  sts_exe=""
  local base ini
  if [ "$OS" = mac ]; then
    [ -d /Applications/SpringToolsForEclipse.app ] && sts_dir=/Applications/SpringToolsForEclipse.app && sts_exe=$sts_dir
    return 0
  fi
  # setup이 푸는 위치, 그리고 직접 압축을 푼 흔한 위치(사용자 폴더, C:\, Program Files)만 본다.
  # 실행 파일 이름은 5.x가 SpringToolsForEclipse, 4.x가 SpringToolSuite4다
  for base in "$(cygpath -u "$LOCALAPPDATA")/Programs" "$HOME" /c "/c/Program Files"; do
    [ -d "$base" ] || continue
    ini=$(find "$base" -maxdepth 3 -iname 'SpringTool*.ini' 2>/dev/null | head -1 || true)
    if [ -n "$ini" ]; then sts_dir=$(dirname "$ini"); sts_exe=$(cygpath -w "${ini%.ini}.exe"); return 0; fi
  done
  return 0
}
install_sts_win() {
  local url tmp dest
  url="https://cdn.spring.io/spring-tools/release/dist/$STS_VERSION.RELEASE/e${STS_ECLIPSE%.*}/spring-tools-for-eclipse-$STS_VERSION.RELEASE-e$STS_ECLIPSE-win32.win32.x86_64.zip"
  # zip 안에 sts-<버전>.RELEASE 폴더가 들어 있어서 Programs 아래에 바로 푼다
  dest="$(cygpath -u "$LOCALAPPDATA")/Programs"
  tmp=$(mktemp -d)
  echo "  STS $STS_VERSION 내려받는 중(약 600MB)..."
  curl -fL --progress-bar -o "$tmp/sts.zip" "$url" || { rm -rf "$tmp"; die "STS를 내려받지 못했습니다. 네트워크를 확인한 뒤 다시 실행해 주세요."; }
  mkdir -p "$dest"
  # Git Bash의 tar는 zip을 못 푸므로 Windows에 들어 있는 tar를 쓴다
  /c/Windows/System32/tar.exe -xf "$(cygpath -w "$tmp/sts.zip")" -C "$(cygpath -w "$dest")" \
    || { rm -rf "$tmp"; die "STS 압축을 풀지 못했습니다."; }
  rm -rf "$tmp"
}
if [ "$ide" != vscode ]; then
  find_sts
  if [ -n "$sts_dir" ]; then
    skip "STS ($sts_exe)"
  else
    if [ "$OS" = mac ]; then pkg springtoolsforeclipse cask; else install_sts_win; fi
    find_sts
    [ -n "$sts_dir" ] || die "STS를 설치했지만 설치 폴더를 찾지 못했습니다."
    ok "STS ($sts_exe)"
  fi
fi

if [ "$NEED_RESTART" = 1 ]; then
  printf '\n[다시 실행] 새로 설치한 프로그램은 새 창에서만 인식됩니다. 창을 닫았다가 새로 열고 같은 명령(setup.bat 또는 bash setup.sh)을 한 번 더 실행해 주세요.\n'
  exit 2
fi

# DB: 팀 공용 DB를 쓴다. 스프링 설정 src/main/resources/application.properties(git 제외)를 디스코드에서 받아 쓴다.
# 공개 저장소이므로 서버 주소와 비밀값을 저장소에 두지 않는다. 키 목록은 application.properties.example 에 있다
APP_PROPS=src/main/resources/application.properties
# key=값 에서 값을 읽는다. 없으면 빈 문자열
prop() { sed -n "s/^$1[[:space:]]*=[[:space:]]*//p" "$APP_PROPS" 2>/dev/null | tr -d '\r' | head -1 || true; }
has_env=n
if [ ! -f "$APP_PROPS" ]; then
  todo "디스코드에서 받은 application.properties 를 $APP_PROPS 에 넣고 다시 실행해 주세요. 내려받을 때 이름이 application.properties.txt 로 바뀌지 않았는지 확인해 주세요"
else
  missing=""
  for key in spring.datasource.url spring.datasource.username spring.datasource.password; do
    [ -n "$(prop "$key")" ] || missing="$missing $key"
  done
  if [ -n "$missing" ]; then
    todo "application.properties 에 값이 없는 키가 있습니다:$missing. 디스코드의 최신 파일로 바꿔 주세요"
  else
    has_env=y
    skip "application.properties (DB 접속 정보)"
  fi
fi

# MCP: DMS(요구사항정의서·화면설계서·기능명세서). DMS 주소는 공개 저장소에 두지 않고 application.properties 의 dms.mcp-url 에서 읽는다
step "MCP"
dms_url=$(prop dms.mcp-url)
! use_claude || claude_ai_list_load
if [ ! -f "$APP_PROPS" ]; then
  :  # application.properties가 없다는 안내는 위에서 했다
elif [ -n "$dms_url" ]; then
  mcp_add dms "$dms_url"
# 주소가 없어도 감지한 도구에 dms가 이미 있으면(Claude Code는 claude.ai 커넥터, Codex는 등록된 것) 할 일로 남기지 않는다
elif { ! use_claude || [ -n "$(claude_connector dms)" ]; } && { ! use_codex || codex mcp get dms >/dev/null 2>&1; }; then
  skip "MCP dms (이미 연결돼 있어 dms.mcp-url 없이 건너뜀)"
else
  todo "application.properties 에 dms.mcp-url 값이 없습니다. 디스코드의 최신 파일로 바꿔 주세요"
fi

step "확인"
./gradlew build -q && ok "./gradlew build"

to_native() { if [ "$OS" = win ]; then cygpath -w "$1"; else printf '%s' "$1"; fi; }
# Gradle이 받아 둔 jar를 찾는다. 경로 중간에 해시 폴더가 있으므로 버전 폴더 이름으로 정렬해 가장 새 것을 고른다
gradle_jar() {
  find "${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1/$1/$2" -name "$2-*.jar" ! -name '*-sources.jar' ! -name '*-javadoc.jar' 2>/dev/null \
    | awk -F/ '{ print $(NF-2) "\t" $0 }' | sort -V | tail -1 | cut -f2
}
# Eclipse 설정 파일(properties 형식)의 키 하나를 바꾸거나 추가한다. 값의 \n 같은 글자가 풀리지 않도록 ENVIRON으로 넘긴다
set_pref() {
  mkdir -p "$(dirname "$1")"
  touch "$1"
  grep -q '^eclipse.preferences.version=' "$1" || printf 'eclipse.preferences.version=1\n' >> "$1"
  K="$2" V="$3" awk -F= '$1 == ENVIRON["K"] { print ENVIRON["K"] "=" ENVIRON["V"]; found = 1; next } { print }
    END { if (!found) print ENVIRON["K"] "=" ENVIRON["V"] }' "$1" > "$1.tmp" && mv "$1.tmp" "$1"
}

# STS: Lombok을 넣고 프로젝트 설정(.settings, git 제외)을 VS Code와 맞춘다
if [ "$ide" != vscode ] && [ -n "$sts_dir" ]; then
  # Eclipse는 Lombok을 따로 넣어야 getter·생성자 등을 인식한다. 빌드 때 Gradle이 받아 둔 lombok.jar로 설치한다
  # ini에 줄만 있고 jar가 없으면(STS 폴더를 옮긴 경우) STS가 실행되지 않으므로 jar가 실제로 있는지까지 본다
  sts_ini=$(find "$sts_dir" -maxdepth 3 -iname 'SpringTool*.ini' | head -1)
  lombok_agent() {
    local p
    p=$(sed -n 's/^-javaagent:\(.*lombok[^/\\]*\.jar\).*/\1/p' "$sts_ini" | tr -d '\r' | head -1)
    [ -n "$p" ] && [ "$OS" = win ] && p=$(cygpath -u "$p")
    [ -n "$p" ] && [ -f "$p" ]
  }
  if lombok_agent; then
    skip "STS Lombok"
  else
    lombok=$(gradle_jar org.projectlombok lombok)
    "$have_jdk/bin/java" -jar "$(to_native "$lombok")" install "$(to_native "$sts_dir")" > /dev/null 2>&1 || true
    if lombok_agent; then ok "STS Lombok ($(basename "$lombok"))"
    else todo "STS에 Lombok을 넣지 못했습니다. STS를 끈 뒤 다시 실행해 주세요"; fi
  fi
  # Windows 바탕화면 바로가기: setup이 설치한 STS(%LOCALAPPDATA%\Programs)만 만든다. zip을 풀기만 해서 시작 메뉴에도 없기 때문이다.
  # 직접 설치한 STS는 바탕화면을 건드리지 않고 위에서 실행 파일 경로만 보여 준다. 맥은 /Applications에 있어 만들지 않는다.
  # 바탕화면이 OneDrive로 옮겨졌을 수 있어 위치는 Windows에 묻는다. 이름이 달라도 STS를 가리키는 바로가기가 있으면 그것을 쓴다
  sts_by_setup=n
  if [ "$OS" = win ]; then
    case "$sts_dir" in "$(cygpath -u "$LOCALAPPDATA")/Programs/"*) sts_by_setup=y ;; esac
  fi
  if [ "$sts_by_setup" = y ]; then
    lnk=$(STS_EXE="$sts_exe" powershell.exe -NoProfile -Command '
      $desk = [Environment]::GetFolderPath("Desktop")
      $shell = New-Object -ComObject WScript.Shell
      $lnk = Get-ChildItem $desk -Filter *.lnk -ErrorAction SilentlyContinue |
        Where-Object { $shell.CreateShortcut($_.FullName).TargetPath -match "SpringTool[^\\]*\.exe$" } |
        Select-Object -First 1 -ExpandProperty FullName
      $state = "updated"
      if (-not $lnk) { $lnk = Join-Path $desk "Spring Tools for Eclipse.lnk"; $state = "created" }
      elseif ($shell.CreateShortcut($lnk).TargetPath -eq $env:STS_EXE) { "same"; exit }
      $s = $shell.CreateShortcut($lnk)
      $s.TargetPath = $env:STS_EXE
      $s.WorkingDirectory = Split-Path $env:STS_EXE
      $s.Save()
      $state' | tr -d '\r')
    case "$lnk" in
      same) skip "STS 바탕화면 바로가기" ;;
      created) ok "STS 바탕화면 바로가기 (Spring Tools for Eclipse)" ;;
      updated) ok "STS 바탕화면 바로가기 (경로 고침)" ;;
      *) todo "STS 바탕화면 바로가기를 만들지 못했습니다: $lnk" ;;
    esac
  fi
  # 포맷은 VS Code와 같은 eclipse-formatter.xml을 쓰고, 저장할 때 포맷한다. 인코딩 UTF-8, 줄바꿈 LF
  while IFS='|' read -r k v; do
    set_pref .settings/org.eclipse.jdt.core.prefs "$k" "$v"
  done < <(sed -n 's/.*<setting id="\([^"]*\)" value="\([^"]*\)".*/\1|\2/p' eclipse-formatter.xml)
  set_pref .settings/org.eclipse.jdt.ui.prefs formatter_profile _nyangmeong
  set_pref .settings/org.eclipse.jdt.ui.prefs formatter_settings_version 23
  set_pref .settings/org.eclipse.jdt.ui.prefs editor_save_participant_org.eclipse.jdt.ui.postsavelistener.cleanup true
  set_pref .settings/org.eclipse.jdt.ui.prefs sp_cleanup.format_source_code true
  set_pref .settings/org.eclipse.jdt.ui.prefs sp_cleanup.format_source_code_changes_only false
  set_pref .settings/org.eclipse.jdt.ui.prefs sp_cleanup.on_save_use_additional_actions false
  set_pref .settings/org.eclipse.core.resources.prefs 'encoding/<project>' UTF-8
  set_pref .settings/org.eclipse.core.runtime.prefs line.separator '\n'
  ok "STS 프로젝트 설정 (eclipse-formatter.xml, 저장 시 포맷, UTF-8, LF)"
  # 가져오기 여부: STS가 마지막으로 연 작업 공간에 이 프로젝트가 등록돼 있는지 본다.
  # .project 파일은 다른 작업 공간에서 가져와도 생기므로 기준이 될 수 없다.
  # 작업 공간 경로는 Java properties 이스케이프(\: \\ \uXXXX, 여러 개면 \n으로 구분)로 저장돼 있어 perl로 풀고 첫 번째를 쓴다
  proj_name=$(sed -n "s/^rootProject.name *= *['\"]\([^'\"]*\)['\"].*/\1/p" settings.gradle)
  ide_prefs=$(find "$sts_dir" -maxdepth 5 -path '*configuration/.settings/org.eclipse.ui.ide.prefs' 2>/dev/null | head -1)
  sts_ws=""
  [ -n "$ide_prefs" ] && sts_ws=$(perl -CS -ne 'next unless s/^RECENT_WORKSPACES=//; s/\r?\n$//; s{\\(?:u([0-9A-Fa-f]{4})|(.))}{defined $1 ? chr(hex $1) : ($2 eq "n" ? "\n" : $2)}ge; print((split /\n/)[0])' "$ide_prefs")
  sts_ws_unix=$sts_ws
  [ -n "$sts_ws" ] && [ "$OS" = win ] && sts_ws_unix=$(cygpath -u "$sts_ws")
  if [ -n "$sts_ws" ] && [ -d "$sts_ws_unix/.metadata/.plugins/org.eclipse.core.resources/.projects/$proj_name" ]; then
    skip "STS 프로젝트 가져오기 (작업 공간 $sts_ws)"
  else
    # 상위 폴더를 고르면 Gradle Wrapper가 없어 STS가 Java 25를 지원하지 않는 Gradle로 동기화하다 실패한다
    project_path=$PWD
    [ "$OS" = win ] && project_path=$(cygpath -w "$PWD")
    todo "STS${sts_ws:+ 작업 공간($sts_ws)}에서 File > Import > Gradle > Existing Gradle Project 로 $project_path 폴더를 가져와 주세요(처음 한 번만). 상위 폴더가 아니라 gradlew 가 있는 이 폴더를 고릅니다"
  fi
fi

# DB 접속: 서버는 첫 쿼리 때 DB에 붙으므로 기동만으로는 확인되지 않는다.
# Spring과 같은 방식(Properties)으로 application.properties를 읽어 JDBC로 직접 접속해 본다. 드라이버는 Gradle이 받아 둔 것을 쓴다
driver=$(gradle_jar com.mysql mysql-connector-j)
check=$(mktemp -d)
cat > "$check/DbCheck.java" <<'JAVA'
import java.io.FileReader;
import java.sql.DriverManager;
import java.util.Properties;

public class DbCheck {
  public static void main(String[] args) throws Exception {
    Properties env = new Properties();
    try (FileReader r = new FileReader(args[0])) { env.load(r); }
    DriverManager.setLoginTimeout(5);
    // 앱은 SQL 로그용 jdbc:log4jdbc: 주소를 쓴다. 여기서는 일반 MySQL 드라이버로 확인하므로 접두어를 뗀다
    String url = env.getProperty("spring.datasource.url").replaceFirst("^jdbc:log4jdbc:", "jdbc:");
    try (var c = DriverManager.getConnection(url,env.getProperty("spring.datasource.username"), env.getProperty("spring.datasource.password"));
         var rs = c.createStatement().executeQuery("SELECT DATABASE(), (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE())")) {
      rs.next();
      System.out.println(rs.getString(1) + ", 테이블 " + rs.getInt(2) + "개");
    } catch (Exception e) {
      System.out.println(e.getMessage().lines().findFirst().orElse(e.toString()));
      System.exit(1);
    }
  }
}
JAVA
if [ "$has_env" = n ]; then
  :  # application.properties가 없거나 비어 있다는 안내는 위에서 했다
elif [ -z "$driver" ]; then
  todo "MySQL 드라이버를 찾지 못해 DB 접속을 확인하지 못했습니다"
elif result=$("$have_jdk/bin/java" -Dstdout.encoding=UTF-8 -cp "$(to_native "$driver")" "$(to_native "$check/DbCheck.java")" "$(to_native "$PWD/$APP_PROPS")"); then
  ok "DB 접속 ($result)"
else
  todo "공용 DB에 접속하지 못했습니다: $result. 네트워크(학원 밖이면 접속이 막힐 수 있음)와 접속 정보를 확인한 뒤 다시 실행해 주세요"
fi
rm -rf "$check"

# 서버가 application.properties를 읽고 뜨는지 보고 끈다. 포트는 비어 있는 아무 포트(0)를 쓴다
if [ "$has_env" = y ]; then
  jar=$(ls build/libs/*.jar | grep -v -- '-plain\.jar$' | head -1)
  log=$(mktemp)
  "$have_jdk/bin/java" -jar "$jar" --server.port=0 > "$log" 2>&1 &
  pid=$!
  for _ in $(seq 1 120); do
    grep -q 'Started .* in ' "$log" && break
    kill -0 "$pid" 2>/dev/null || break
    sleep 1
  done
  started=$(grep -o 'Started .* in [0-9.]* seconds' "$log" || true)
  kill "$pid" 2>/dev/null || true
  wait "$pid" 2>/dev/null || true
  if [ -z "$started" ]; then
    tail -40 "$log"
    rm -f "$log"
    die "서버가 뜨지 않았습니다. 위 로그를 확인해 주세요."
  fi
  rm -f "$log"
  ok "서버 기동 (${started#Started })"
fi

printf '\n설치가 끝났습니다. 직접 할 일 %d개(위 [직접] 줄).\n' "$TODO"
# 직접 할 일이 남으면 0이 아닌 코드로 끝낸다
[ "$TODO" = 0 ] || exit 3
