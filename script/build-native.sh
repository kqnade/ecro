#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")/.."

echo "Building Rust cdylib..."
./script/build-rust.sh

echo "Building GraalVM native image..."
clojure -T:build aot

native-image \
  -cp "$(clojure -Spath):target/classes" \
  -H:Name=ecro \
  -H:ConfigurationFileDirectories=resources/META-INF/native-image \
  --initialize-at-build-time \
  --initialize-at-run-time=com.sun.jna.internal.Cleaner,com.sun.jna.Native,com.sun.jna.NativeLibrary \
  --no-fallback \
  -Dclojure.compiler.direct-linking=true \
  ecro.main

echo "Native image build complete: ./ecro"
