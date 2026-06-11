#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
RUST_DIR="${PROJECT_DIR}/rust/ecro-core"
RUST_TARGET_DIR="${RUST_DIR}/target/release"

case "$(uname -s)" in
  Darwin) LIB_NAME="libecro_core.dylib" ;;
  Linux) LIB_NAME="libecro_core.so" ;;
  *) LIB_NAME="libecro_core.so" ;;
esac

if [[ ! -f "${RUST_TARGET_DIR}/${LIB_NAME}" ]]; then
  (cd "${RUST_DIR}" && cargo build --release)
fi

export LD_LIBRARY_PATH="${RUST_TARGET_DIR}:${LD_LIBRARY_PATH:-}"
export DYLD_LIBRARY_PATH="${RUST_TARGET_DIR}:${DYLD_LIBRARY_PATH:-}"

export JAVA_HOME="${JAVA_HOME:-$(mise which java 2>/dev/null | xargs dirname 2>/dev/null | xargs dirname 2>/dev/null || echo /usr/lib/jvm/default-java)}"

cd "${PROJECT_DIR}"

exec clojure -J--enable-native-access=ALL-UNNAMED -J-Djna.library.path="${RUST_TARGET_DIR}" -M -m ecro.main "$@"
