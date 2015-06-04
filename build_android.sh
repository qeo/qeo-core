#!/bin/bash
# Copyright (c) 2015 - Qeo LLC
#
# The source code form of this Qeo Open Source Project component is subject
# to the terms of the Clear BSD license.
#
# You can redistribute it and/or modify it under the terms of the Clear BSD
# License (http://directory.fsf.org/wiki/License:ClearBSD). See LICENSE file
# for more details.
#
# The Qeo Open Source Project also includes third party Open Source Software.
# See LICENSE file for more details.


#build qeo-android and all dependencies

SELF_DIR=$(cd $(dirname $0) > /dev/null; pwd)
function syntax()
{
    echo "Usage: $(basename $0) <dir where to put all generated binaries>"
    exit 1
}

if [ $# -ne 1 ]; then
    syntax
fi

GRADLE="gradle -DqeoGradleHelper=$SELF_DIR/gradle-helper -Dartifactory-overlay=$SELF_DIR/$1/maven-repo -DqeoUseMavenCentral=1"

mkdir -p $SELF_DIR/$1

set -e

echo "Compiling qeo-codegen"
cd "${SELF_DIR}/qeo-codegen"
$GRADLE assemble uploadOverlay

echo "Compiling qeo-qdm"
cd "${SELF_DIR}/qeo-qdm"
$GRADLE assemble uploadOverlay

echo "Compiling qeo-java"
cd "${SELF_DIR}/qeo-java"
if [ ! -e "qeo-java/output" ]; then
    mkdir -p ../qeo-c/output
    ln -s ../../qeo-c/output qeo-java/output
fi
$GRADLE assembleAndroid uploadOverlay

echo "Compiling qeo-android"
cd "${SELF_DIR}/qeo-android"
$GRADLE assemble uploadOverlay

