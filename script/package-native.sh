#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
RUST_TARGET_DIR="${PROJECT_DIR}/rust/ecro-core/target/release"

if [[ $# -ne 1 ]]; then
  echo "Usage: $0 <archive-path>" >&2
  exit 2
fi

ARCHIVE_PATH="$1"
BINARY_PATH="${ECRO_BINARY_PATH:-${PROJECT_DIR}/ecro}"

if [[ -n "${ECRO_LIBRARY_PATH:-}" ]]; then
  LIBRARY_PATH="${ECRO_LIBRARY_PATH}"
else
  case "$(uname -s)" in
    Darwin) LIBRARY_PATH="${RUST_TARGET_DIR}/libecro_core.dylib" ;;
    Linux) LIBRARY_PATH="${RUST_TARGET_DIR}/libecro_core.so" ;;
    *)
      echo "Unsupported operating system: $(uname -s)" >&2
      exit 2
      ;;
  esac
fi

if [[ ! -f "${BINARY_PATH}" ]]; then
  echo "Native executable not found: ${BINARY_PATH}" >&2
  exit 1
fi

if [[ ! -f "${LIBRARY_PATH}" ]]; then
  echo "Rust shared library not found: ${LIBRARY_PATH}" >&2
  exit 1
fi

LIBRARY_NAME="$(basename "${LIBRARY_PATH}")"
case "${LIBRARY_NAME}" in
  libecro_core.so | libecro_core.dylib) ;;
  *)
    echo "Unexpected Rust shared library name: ${LIBRARY_NAME}" >&2
    exit 1
    ;;
esac

STAGING_DIR="$(mktemp -d "${TMPDIR:-/tmp}/ecro-native-package.XXXXXX")"
trap 'rm -rf "${STAGING_DIR}"' EXIT

cp "${BINARY_PATH}" "${STAGING_DIR}/ecro"
cp "${LIBRARY_PATH}" "${STAGING_DIR}/${LIBRARY_NAME}"
chmod +x "${STAGING_DIR}/ecro"

tar -czf "${ARCHIVE_PATH}" -C "${STAGING_DIR}" ecro "${LIBRARY_NAME}"

echo "Native package created: ${ARCHIVE_PATH}"
