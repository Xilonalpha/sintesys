#!/bin/sh
set -eu
# Run this from the project root after installing any Gradle 8.7+ distribution.
# It regenerates the official wrapper JAR instead of using a hand-made binary.
gradle wrapper --gradle-version 8.7 --distribution-type bin
