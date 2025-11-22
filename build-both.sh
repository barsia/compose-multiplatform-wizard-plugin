#!/bin/bash
set -e

echo "========================================"
echo "Building Compose Multiplatform Wizard"
echo "========================================"

echo ""
echo "1/2 Building for Android Studio 2025.2+..."
./gradlew clean buildPlugin
echo "✅ Android Studio version built"

echo ""
echo "2/2 Building for IntelliJ IDEA 2025.3+..."
./gradlew buildPlugin -PrunIntellijIdea=true
echo "✅ IntelliJ IDEA version built"

echo ""
echo "========================================"
echo "Build Complete!"
echo "========================================"
echo ""
echo "Output files:"
ls -lh build/distributions/*.zip
echo ""
echo "Install instructions:"
echo "  - For IntelliJ IDEA 2025.3+: compose-multiplatform-wizard-plugin-1.0.0-ij.zip"
echo "  - For Android Studio 2025.2+: compose-multiplatform-wizard-plugin-1.0.0-ai.zip"
