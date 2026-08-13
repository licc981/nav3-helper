#!/usr/bin/env bash

set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
project_dir="$(CDPATH= cd -- "$script_dir/.." && pwd)"
version_file="$project_dir/gradle/libs.versions.toml"
gradle_properties="${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties"
backslash='\'

usage() {
  echo "Usage: $0 <version>" >&2
  echo "Example: $0 1.0.1" >&2
}

has_property() {
  local property_name="$1"
  local environment_name="ORG_GRADLE_PROJECT_${property_name}"

  if [[ -n "${!environment_name:-}" ]]; then
    return 0
  fi

  [[ -f "$gradle_properties" ]] && grep -Eq "^[[:space:]]*${property_name}[[:space:]]*=" "$gradle_properties"
}

read_property() {
  local property_name="$1"
  local environment_name="ORG_GRADLE_PROJECT_${property_name}"

  if [[ -n "${!environment_name:-}" ]]; then
    printf '%s' "${!environment_name}"
    return
  fi

  sed -nE "s/^[[:space:]]*${property_name}[[:space:]]*=[[:space:]]*(.*)$/\\1/p" "$gradle_properties" | head -n 1
}

# .properties 里的值是 Java properties 转义过的：换行是 \n，"=" 和 ":" 会写成 \= 和 \:。
# 只还原 \n 会把 base64 尾部的 \=\= 和 CRC 行的 \= 留下来，导致 gpg 无法解析。
unescape_properties_value() {
  awk '
    {
      out = ""
      i = 1
      n = length($0)
      while (i <= n) {
        c = substr($0, i, 1)
        if (c == "\\" && i < n) {
          d = substr($0, i + 1, 1)
          if (d == "n") { out = out "\n" }
          else if (d == "t") { out = out "\t" }
          else if (d == "r") { out = out "" }
          else { out = out d }
          i += 2
          continue
        }
        out = out c
        i++
      }
      print out
    }
  '
}

normalize_signing_key() {
  local value="$1"
  value="${value//$'\r'/}"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"

  # 合法的 armor 私钥不含反斜杠，出现反斜杠说明是转义过的写法
  if [[ "$value" == *"$backslash"* ]]; then
    value="$(printf '%s\n' "$value" | unescape_properties_value)"
  fi

  printf '%s' "$value"
}

base64_decode() {
  local input
  input="$(cat)"
  printf '%s' "$input" | base64 -d 2>/dev/null ||
    printf '%s' "$input" | base64 -D 2>/dev/null ||
    true
}

# 导入到一次性 keyring 里读取密钥长 ID，同时充当私钥格式校验
derive_key_id() {
  local key="$1"
  local signing_gpg_home
  local key_id=""

  signing_gpg_home="$(mktemp -d "${TMPDIR:-/tmp}/nav3-signing.XXXXXX")"
  chmod 700 "$signing_gpg_home"
  key_id="$(
    printf '%s\n' "$key" |
      gpg --homedir "$signing_gpg_home" --batch --with-colons \
        --import-options show-only --import 2>/dev/null |
      awk -F: '$1 == "sec" { print $5; exit }'
  )" || key_id=""
  rm -rf "$signing_gpg_home"

  printf '%s' "$key_id"
}

# 给去掉了 armor 头尾的单行 base64 重新加上头尾
wrap_armor() {
  local body="$1"
  local checksum=""
  body="${body//[[:space:]]/}"

  # armor 末尾的 CRC 是 "=" 加 4 位 base64，必须单独成行
  if [[ "$body" =~ ^(.*)(=[A-Za-z0-9+/]{4})$ ]]; then
    body="${BASH_REMATCH[1]}"
    checksum="${BASH_REMATCH[2]}"
  fi

  printf -- '-----BEGIN PGP PRIVATE KEY BLOCK-----\n\n'
  printf '%s\n' "$body" | fold -w 64
  if [[ -n "$checksum" ]]; then
    printf '%s\n' "$checksum"
  fi
  printf -- '-----END PGP PRIVATE KEY BLOCK-----\n'
}

configure_signing_properties() {
  local signing_key
  local signing_key_id=""
  local decoded

  signing_key="$(normalize_signing_key "$(read_property signingInMemoryKey)")"

  # 兼容对整段 armor 再做一层 base64 的写法
  if [[ "$signing_key" != *"BEGIN PGP"* ]]; then
    decoded="$(printf '%s' "$signing_key" | base64_decode)"
    if [[ "$decoded" == *"BEGIN PGP"* ]]; then
      signing_key="$decoded"
    fi
  fi

  if command -v gpg >/dev/null 2>&1; then
    signing_key_id="$(derive_key_id "$signing_key")"

    # 兼容去掉 armor 头尾的单行 base64 写法
    if [[ -z "$signing_key_id" && "$signing_key" != *"BEGIN PGP"* ]]; then
      signing_key="$(wrap_armor "$signing_key")"
      signing_key_id="$(derive_key_id "$signing_key")"
    fi

    if [[ -z "$signing_key_id" ]]; then
      echo "signingInMemoryKey 不是 gpg 可解析的私钥。" >&2
      echo "重新导出：gpg --armor --export-secret-keys <KEY_ID>" >&2
      echo "可接受的格式：完整 ASCII-armored 私钥（真实多行，或 .properties 风格的 \\n 单行）、" >&2
      echo "去掉 armor 头尾的单行 base64、或对整段 armor 再做一层 base64。" >&2
      exit 1
    fi
  else
    echo "警告：未找到 gpg，跳过 signingInMemoryKey 校验。" >&2
  fi

  # 用环境变量传给 Gradle：既不会出现在进程命令行里，也绕开 .properties 转义差异。
  # 未显式配置 signingInMemoryKeyId 时不传，Gradle 会直接用私钥里的密钥。
  export ORG_GRADLE_PROJECT_signingInMemoryKey="$signing_key"

  # Gradle 只按 8 位短 ID 匹配密钥，传 16 位长 ID 会报 "Could not read PGP secret key"
  if has_property signingInMemoryKeyId; then
    local configured_key_id
    configured_key_id="$(read_property signingInMemoryKeyId)"
    configured_key_id="${configured_key_id//[[:space:]]/}"
    if (( ${#configured_key_id} > 8 )); then
      echo "提示：signingInMemoryKeyId 长度为 ${#configured_key_id}，已截取后 8 位供 Gradle 匹配。" >&2
      export ORG_GRADLE_PROJECT_signingInMemoryKeyId="${configured_key_id: -8}"
    fi
  fi
}

if [[ $# -ne 1 || -z "$1" ]]; then
  usage
  exit 2
fi

requested_version="$1"
configured_version="$(sed -nE 's/^navHelper[[:space:]]*=[[:space:]]*"([^"]+)".*/\1/p' "$version_file")"

if [[ -z "$configured_version" ]]; then
  echo "Could not read navHelper from $version_file" >&2
  exit 1
fi

if [[ "$requested_version" != "$configured_version" ]]; then
  echo "Requested version $requested_version does not match configured version $configured_version." >&2
  echo "Update navHelper in gradle/libs.versions.toml before publishing." >&2
  exit 1
fi

central_base_url="https://repo.maven.apache.org/maven2/io/github/licc981"
published_pom_url="$central_base_url/navigation3-helper/$requested_version/navigation3-helper-$requested_version.pom"
http_status="$(curl --silent --show-error --location --output /dev/null --write-out '%{http_code}' "$published_pom_url")"

case "$http_status" in
  200)
    echo "Version $requested_version is already available on Maven Central." >&2
    echo "Choose a new navHelper version before publishing." >&2
    exit 1
    ;;
  404)
    ;;
  *)
    echo "Could not confirm whether version $requested_version already exists (HTTP $http_status)." >&2
    echo "Check Maven Central connectivity and try again." >&2
    exit 1
    ;;
esac

required_properties=(
  mavenCentralUsername
  mavenCentralPassword
  signingInMemoryKey
  signingInMemoryKeyPassword
)

missing_properties=()
for property_name in "${required_properties[@]}"; do
  if ! has_property "$property_name"; then
    missing_properties+=("$property_name")
  fi
done

if (( ${#missing_properties[@]} > 0 )); then
  echo "Missing Gradle publishing properties:" >&2
  printf '  - %s\n' "${missing_properties[@]}" >&2
  echo "Configure them in ~/.gradle/gradle.properties or as ORG_GRADLE_PROJECT_* environment variables." >&2
  exit 1
fi

configure_signing_properties

cd "$project_dir"

bash ./gradlew \
  :navigation3-helper:jvmTest \
  :nav3-ksp-compiler:jvmTest \
  :navigation3-helper:generatePomFileForKotlinMultiplatformPublication \
  :nav3-ksp-compiler:generatePomFileForKotlinMultiplatformPublication

bash ./gradlew publishAndReleaseToMavenCentral
