#!/bin/bash
set -e

echo "=========================================="
echo "Building automation with Maven..."
echo "=========================================="

cd "$(dirname "$0")"

# Try to find Maven
MVN=$(which mvn 2>/dev/null) || MVN="/usr/bin/mvn"

if [ ! -f "$MVN" ]; then
    echo "Error: Maven not found. Installing..."
    sudo apt-get update && sudo apt-get install -y maven
    MVN="/usr/bin/mvn"
fi

echo "Using Maven: $MVN"
echo ""

# Build
$MVN clean package -DskipTests

echo ""
echo "=========================================="
echo "Build successful! Running app..."
echo "=========================================="
echo ""

# Run
java -jar target/automation-1.0-SNAPSHOT.jar
