#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/../rust/ecro-core"

echo "Building Rust cdylib..."
cargo build --release

echo "Generating C headers with cbindgen..."
cbindgen --config cbindgen.toml --crate ecro-core --output ecro_core.h

echo "Rust build complete."
