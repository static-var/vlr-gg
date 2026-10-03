#!/bin/bash
set -euo pipefail
: "${JAVA_HOME:?Set JAVA_HOME to JDK 21}"
: "${ANDROID_SDK_ROOT:?Set ANDROID_SDK_ROOT}"
export ANDROIDX_JDK21="$JAVA_HOME"
source_dir="${1:?Pass isolated Compose source directory}"
repo_dir="${2:?Pass output Maven repository directory}"
cd "$source_dir"
build-brief ./gradlew \
  :compose:ui:ui:publishKotlinMultiplatformPublicationToMavenLocal \
  :compose:ui:ui:publishIosArm64PublicationToMavenLocal \
  :compose:ui:ui:publishIosSimulatorArm64PublicationToMavenLocal \
  :compose:ui:ui-uikit:publishKotlinMultiplatformPublicationToMavenLocal \
  :compose:ui:ui-uikit:publishIosArm64PublicationToMavenLocal \
  :compose:ui:ui-uikit:publishIosSimulatorArm64PublicationToMavenLocal \
  -Pcompose.platforms=ios \
  -Pjetbrains.publication.version.COMPOSE=1.12.0 \
  -Pvlr.accessibility.version=1.12.0-vlr-a11y1 \
  "-Dmaven.repo.local=$repo_dir" "-Dorg.gradle.java.home=$JAVA_HOME" \
  --max-workers=4 --no-configuration-cache --no-configure-on-demand
