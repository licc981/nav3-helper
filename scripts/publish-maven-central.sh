#!/usr/bin/env bash

set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
project_dir="$(CDPATH= cd -- "$script_dir/.." && pwd)"
version_file="$project_dir/gradle/libs.versions.toml"
gradle_properties="${GRADLE_USER_HOME:-$HOME/.gradle}/gradle.properties"

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

configure_signing_properties() {
  local signing_key
  local signing_key_id
  local signing_key_password
  local signing_gpg_home
  signing_key="$(read_property signingInMemoryKey)"
  signing_key="${signing_key//\\n/$'\n'}"
  signing_key_password="$(read_property signingInMemoryKeyPassword)"

  if has_property signingInMemoryKeyId; then
    signing_key_id="$(read_property signingInMemoryKeyId)"
  else
    if ! command -v gpg >/dev/null 2>&1; then
      echo "GPG is required to derive signingInMemoryKeyId from signingInMemoryKey." >&2
      exit 1
    fi

    signing_gpg_home="$(mktemp -d "${TMPDIR:-/tmp}/nav3-signing.XXXXXX")"
    chmod 700 "$signing_gpg_home"
    signing_key_id="$(
      printf '%s\n' "$signing_key" |
        gpg --homedir "$signing_gpg_home" --batch --with-colons \
          --import-options show-only --import 2>/dev/null |
        awk -F: '$1 == "sec" { print $5; exit }'
    )" || signing_key_id=""
    rm -rf "$signing_gpg_home"
  fi

  if [[ -z "$signing_key_id" ]]; then
    echo "Could not derive a key ID from signingInMemoryKey." >&2
    echo "Ensure it contains a valid ASCII-armored GPG private key." >&2
    exit 1
  fi

  gradle_signing_args=(
    "-PsigningInMemoryKey=$signing_key"
    "-PsigningInMemoryKeyId=$signing_key_id"
    "-PsigningInMemoryKeyPassword=$signing_key_password"
  )
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

gradle_signing_args=()
configure_signing_properties

cd "$project_dir"

bash ./gradlew \
  :navigation3-helper:jvmTest \
  :nav3-ksp-compiler:jvmTest \
  :navigation3-helper:generatePomFileForKotlinMultiplatformPublication \
  :nav3-ksp-compiler:generatePomFileForKotlinMultiplatformPublication

bash ./gradlew "${gradle_signing_args[@]}" publishAndReleaseToMavenCentral
