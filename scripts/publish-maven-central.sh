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

cd "$project_dir"

bash ./gradlew \
  :navigation3-helper:jvmTest \
  :nav3-ksp-compiler:jvmTest \
  :navigation3-helper:generatePomFileForKotlinMultiplatformPublication \
  :nav3-ksp-compiler:generatePomFileForKotlinMultiplatformPublication

bash ./gradlew publishAndReleaseToMavenCentral
