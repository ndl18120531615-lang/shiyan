#!/usr/bin/env bash
MVNW_DIR="$(dirname "$0")/.mvn/wrapper"
MAVEN_VERSION="3.9.6"
ZIP_NAME="apache-maven-${MAVEN_VERSION}-bin.zip"
if [ ! -d "$MVNW_DIR/apache-maven-${MAVEN_VERSION}" ]; then
  echo "Maven distribution not found in $MVNW_DIR - attempting download..."
  mkdir -p "$MVNW_DIR"
  curl -fsSL -o "$MVNW_DIR/$ZIP_NAME" "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/${ZIP_NAME}" || { echo "Download failed"; exit 1; }
  unzip -q "$MVNW_DIR/$ZIP_NAME" -d "$MVNW_DIR" || { echo "Unzip failed"; exit 1; }
  rm -f "$MVNW_DIR/$ZIP_NAME"
fi
export MAVEN_HOME="$MVNW_DIR/apache-maven-${MAVEN_VERSION}"
export PATH="$MAVEN_HOME/bin:$PATH"
exec mvn "$@"
