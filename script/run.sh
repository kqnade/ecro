#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

export LD_LIBRARY_PATH="${PROJECT_DIR}/rust/ecro-core/target/release:${LD_LIBRARY_PATH:-}"

export JAVA_HOME="${JAVA_HOME:-$(mise which java 2>/dev/null | xargs dirname 2>/dev/null | xargs dirname 2>/dev/null || echo /usr/lib/jvm/default-java)}"

cd "${PROJECT_DIR}"

exec clojure -M -m ecro.main "$@"
