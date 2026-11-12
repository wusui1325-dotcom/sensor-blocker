#!/bin/sh
# Simplified gradlew for CI
exec java -jar "$(dirname "$0")/gradle/wrapper/gradle-wrapper.jar" "$@"
